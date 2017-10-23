package pluginPackage;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NewActionsForPlayButton extends AbstractProjectComponent {
    String remotePath = "http://est_espol@200.10.150.91/est_espol/Fundamentos.git";

    public NewActionsForPlayButton(@NotNull Project project) {
        super(project);
        project.getMessageBus().connect().subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override
            public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
                Boolean conexion = true;
                UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider( "est_espol", "gPw19KX3_" );

                //Configurando workspace
                ConfigureSettings workspaceFile = new ConfigureSettings(project);
                File outputFile = new File(project.getBasePath() + workspaceFile.consoleOutputPath);
                workspaceFile.readXML(project);

                //Inicializa las variables para control de repositorio
                Repository localRepo = null;
                Git git;

                try {
                    localRepo = new FileRepository(workspaceFile.folderPath + "/.git");
                } catch (java.io.IOException err){
                    err.printStackTrace();
                }
                git = new Git(localRepo);

                //Prueba de conexion con servidor
                try{
                    LsRemoteCommand lscommand = git.lsRemote();
                    lscommand.setRemote(remotePath).setCredentialsProvider(credentials).call();
                } catch (TransportException tex){
                    conexion = false;
                    System.out.println("Se ha perdido conexion con el servidor");
                } catch (GitAPIException ex){
                    System.out.println("Error general con JGit");
                }

                //Caso en el que nunca se pudo clonar del repositorio al iniciar el proyecto
                if(!StartUpActions.contieneGit){
                    //Copiando en carpeta temporal
                    File source = new File(workspaceFile.folderPath);
                    File dest = new File(workspaceFile.tempPath);
                    try {
                        FileUtils.copyDirectoryStructure(source, dest);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //Borrando contenido para intentar un git clone
                    try {
                        FileUtils.cleanDirectory(workspaceFile.folderPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        CloneCommand cloneCommand = Git.cloneRepository();
                        cloneCommand.setURI(remotePath);
                        cloneCommand.setCredentialsProvider(credentials);
                        cloneCommand.setDirectory(new File(workspaceFile.folderPath)).call();
                    } catch (TransportException tr){
                        conexion = false;
                        System.out.println("No hay conexion");
                    } catch (JGitInternalException jex){
                        //La carpeta ya existe y tiene un .git asociado
                        System.out.println("Esta carpeta ya contiene un .git asociado.");
                    } catch (GitAPIException ex){
                        System.out.println("Error general de JGit. Lanzado por CloneCommand.call();.");
                    }

                    //Realiza pull del branch perteneciente al ID de usuario, por si en remote ya tiene algo que en esta
                    //máquina no tenga. No se realiza cuando no existe conexión con el remote o con internet en general.
                    if (conexion){
                        //StartUpActions.conexion = true;
                        try {
                            git.pull().setRemoteBranchName(StartUpActions.matricula).setCredentialsProvider(credentials).call();
                        } catch (RefNotAdvertisedException a){
                            System.out.println("Remote " + StartUpActions.matricula + " no existe");
                        } catch (GitAPIException ex){
                            //ex.printStackTrace();
                            System.out.println("Error general con JGit");
                        }
                    }

                    //Regresando contenido a origen
                    source = new File(workspaceFile.tempPath);
                    dest = new File(workspaceFile.folderPath);
                    try {
                        FileUtils.copyDirectoryStructure(source, dest);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //Borrando contenido de temporal para evitar duplicados
                    try {
                        FileUtils.deleteDirectory(workspaceFile.tempPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
                workspaceFile.readXML(project);
                CopyFilesFromType copyFiles = new CopyFilesFromType();
                Date dateobj = copyFiles.copy("py", project.getBasePath(), workspaceFile.projectPath);

                try {
                    Thread.sleep(3000);
                    File log = new File(workspaceFile.logPath);
                    BufferedReader br = null;
                    FileReader fr = null;

                    //Punteros para escribir en archivo log.log, para no sobreescribir se pone append: true
                    FileWriter fileWriter = new FileWriter(workspaceFile.logPath, true);
                    PrintWriter printWriter = new PrintWriter(fileWriter);
                    printWriter.print("<log_project id ='"+ copyFiles.df.format(dateobj) +"' date ='" + df.format(dateobj) + "' name ='" + project.getName() + "'>\n");
                    fr = new FileReader(project.getBasePath() + workspaceFile.consoleOutputPath);
                    br = new BufferedReader(fr);

                    String sCurrentLine;

                    //Copiando contenido de output.txt a log.log
                    while ((sCurrentLine = br.readLine()) != null) {
                        printWriter.print(sCurrentLine + "\n");
                    }
                    printWriter.print("</log_project>\n");
                    printWriter.close();

                } catch (IOException | InterruptedException ex) {
                    ex.printStackTrace();
                }

                //Git 'checkout', git 'add', git 'commit'... No se realiza si no hay conexion.
                if (conexion){
                    try {
                        git.checkout().setName(StartUpActions.matricula).call();
                        git.add().addFilepattern(".").call();
                        git.commit().setMessage("Nuevo commit, fecha: " + dateobj.toString()).call();
                    } catch (Exception ex){
                        //ex.printStackTrace();
                        System.out.println("Referencia " + StartUpActions.matricula + " no existe en remote");
                    }

                    //Por si no existe en remote el branch, se debe de crear. En caso de existir, imprime el error
                    try {
                        CreateBranchCommand bcc = git.branchCreate();
                        bcc.setName(StartUpActions.matricula)
                                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                                .setStartPoint("origin/master")
                                .setForce(false)
                                .call();
                    } catch(GitAPIException ex){
                        //ex.printStackTrace();
                        System.out.println("Trabajando en branch existente.");
                    }

                    //'Push' a 'remote.'
                    try {
                        PushCommand pushCommand = git.push();
                        pushCommand.setRemote("origin");
                        pushCommand.setRefSpecs( new RefSpec(StartUpActions.matricula) );
                        pushCommand.setCredentialsProvider(credentials);
                        pushCommand.call();
                        git.close();
                    } catch (GitAPIException ex){
                        ex.printStackTrace();
                    }
                }
            }
        });
    }
}