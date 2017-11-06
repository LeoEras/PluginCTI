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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
                boolean workday = GlobalTime.isWorkingTime("10:15:00", "11:45:00");
                boolean conexion = true;

                if(workday){
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
                        System.out.println("Se ha perdido conexion con el servidor.");
                    } catch (GitAPIException ex){
                        System.out.println("Error general con JGit. Llamada desde LsRemoteCommand.call();.");
                    }

                    //Si no existe un .git asociado es porque la operacion 'clone' no se pudo realizar.
                    if(!StartUpActions.contieneGit){
                        //Copiando datos a carpeta temporal.
                        File source = new File(workspaceFile.folderPath);
                        File dest = new File(workspaceFile.tempPath);
                        try {
                            FileUtils.copyDirectoryStructure(source, dest);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Borrando contenido para intentar un git 'clone' en carpeta vacía.
                        try {
                            FileUtils.cleanDirectory(workspaceFile.folderPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Git 'clone.'
                        try {
                            CloneCommand cloneCommand = Git.cloneRepository();
                            cloneCommand.setURI(remotePath);
                            cloneCommand.setCredentialsProvider(credentials);
                            cloneCommand.setDirectory(new File(workspaceFile.folderPath)).call();
                        } catch (TransportException tr){
                            conexion = false;
                            System.out.println("No hay conexión.");
                        } catch (JGitInternalException | GitAPIException ex){
                            conexion = false;
                            System.out.println("Error general de JGit. Lanzado por CloneCommand.call();.");
                        }

                        //Realiza pull del 'branch' perteneciente al ID de usuario, por si 'remote' ya existe algo que 'local'
                        //no tenga. Debido a que es una operación lenta, se pregunta por la conexión antes de realizarla.
                        if (conexion){
                            try {
                                git.pull().setRemoteBranchName(StartUpActions.matricula).setCredentialsProvider(credentials).call();
                            } catch (RefNotAdvertisedException a){
                                System.out.println("Remote " + StartUpActions.matricula + " no existe.");
                            } catch (GitAPIException ex){
                                System.out.println("Error general con JGit");
                            }
                        }

                        //Regresando contenido a origen.
                        source = new File(workspaceFile.tempPath);
                        dest = new File(workspaceFile.folderPath);
                        try {
                            FileUtils.copyDirectoryStructure(source, dest);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Borrando contenido temporal para evitar duplicados.
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

                    File log = new File(workspaceFile.logPath);
                    BufferedReader br = null;
                    FileWriter fw = null;
                    FileReader fr = null;
                    PrintWriter pw = null;

                    try {
                        if(!log.exists()){
                            log = new File(workspaceFile.logPath);
                            log.createNewFile();

                            fw = new FileWriter(workspaceFile.logPath, true);
                            pw = new PrintWriter(fw);
                            pw.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n");
                            pw.print("<output>" + "\n");
                            pw.print("</output>");
                            pw.close();
                        }
                    } catch(IOException ex) {
                        System.out.println("Error con manejo de archivos.");
                    }

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ex){
                        ex.printStackTrace();
                    }

                    try{
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(workspaceFile.logPath);
                        Element root = doc.getDocumentElement();
                        Element eNewComponent = doc.createElement("log_project");
                        eNewComponent.setAttribute("id", "" + copyFiles.df.format(dateobj));
                        eNewComponent.setAttribute("date", "" + df.format(dateobj));
                        eNewComponent.setAttribute("name_project", "" + project.getName());

                        fr = new FileReader(project.getBasePath() + workspaceFile.consoleOutputPath);
                        br = new BufferedReader(fr);

                        //Copiando contenido de output.txt a log.log.
                        String sCurrentLine;
                        while ((sCurrentLine = br.readLine()) != null) {
                            eNewComponent.appendChild(doc.createTextNode("" + sCurrentLine + "\n"));
                        }

                        root.appendChild(eNewComponent);
                        TransformerFactory transformerFactory = TransformerFactory.newInstance();
                        Transformer transformer = transformerFactory.newTransformer();
                        //transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                        //transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                        DOMSource source = new DOMSource(doc);
                        StreamResult result = new StreamResult(new File(workspaceFile.logPath));
                        transformer.transform(source, result);
                    } catch (ParserConfigurationException | SAXException | TransformerException | IOException ex){
                        ex.printStackTrace();
                        System.out.println(ex.getMessage());
                    }

                    //Git 'checkout', git 'add', git 'commit'... No se realiza si no hay conexion.
                    if (conexion){
                        try {
                            git.checkout().setName(StartUpActions.matricula).call();
                            git.add().addFilepattern(".").call();
                            git.commit().setMessage("Nuevo commit, fecha: " + dateobj.toString()).call();
                        } catch (Exception ex){
                            System.out.println("Referencia " + StartUpActions.matricula + " no existe en 'remote.'");
                        }

                        //Por si no existe en remote el branch, se debe de crear. En caso de existir, imprime el mensaje
                        //correspondiente en consola.
                        try {
                            CreateBranchCommand bcc = git.branchCreate();
                            bcc.setName(StartUpActions.matricula)
                                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                                    .setStartPoint("origin/master")
                                    .setForce(false)
                                    .call();
                        } catch(GitAPIException ex){
                            //ex.printStackTrace();
                            System.out.println("Trabajando en 'branch' existente.");
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
            }
        });
    }
}