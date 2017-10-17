package pluginPackage;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
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
    public NewActionsForPlayButton(@NotNull Project project) {
        super(project);
        project.getMessageBus().connect().subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override
            public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
                /*RunProfile profile = env.getRunProfile();
                if (profile instanceof ApplicationConfiguration) {

                }*/
                String path = StartUpActions.npath;
                UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider( "est_espol", "gPw19KX3_" );
                String logpath = path + "\\" + project.getName() + "\\" + "log.log";

                //Configurando workspace
                ConfigureWorkspaceFile workspaceFile = new ConfigureWorkspaceFile();
                File outputFile = new File(project.getBasePath() + workspaceFile.consoleOutputPath);
                //workspaceFile.createFile(outputFile);
                workspaceFile.readXML(project);

                //Inicializa las variables para control de repositorio
                Repository localRepo = null;
                Git git;

                try {
                    localRepo = new FileRepository(path + "/.git");
                } catch (java.io.IOException err){
                    err.printStackTrace();
                }
                git = new Git(localRepo);
                DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
                Date dateobj = new Date();
                workspaceFile.readXML(project);

                try {
                    Thread.sleep(3000);
                    File log = new File(logpath);
                    BufferedReader br = null;
                    FileReader fr = null;

                    //Punteros para escribir en archivo log.log, para no sobreescribir se pone append: true
                    FileWriter fileWriter = new FileWriter(logpath, true);
                    PrintWriter printWriter = new PrintWriter(fileWriter);

                    printWriter.print("\n" + df.format(dateobj) + "\t");
                    fr = new FileReader(project.getBasePath() + workspaceFile.consoleOutputPath);
                    br = new BufferedReader(fr);

                    String sCurrentLine;

                    while ((sCurrentLine = br.readLine()) != null) {
                        printWriter.print(sCurrentLine + "\n");
                    }

                    printWriter.close();

                } catch (IOException | InterruptedException ex) {
                    ex.printStackTrace();
                }

                new CopyFilesFromType().copy("py", project.getBasePath(), path + "\\" + project.getName());

                //Git checkout indica al branch al cual se realizará el push
                try {
                    git.checkout().setName(StartUpActions.matr).call();
                    git.add().addFilepattern(".").call();
                    git.commit().setMessage("Nuevo commit, fecha: " + dateobj.toString()).call();
                } catch (Exception ex){
                    //ex.printStackTrace();
                    System.out.println("Referencia " + StartUpActions.matr + " no existe en remote");
                }

                //Por si no existe en remote el branch, se debe de crear. En caso de existir, imprime el error
                try {
                    CreateBranchCommand bcc = git.branchCreate();
                    bcc.setName(StartUpActions.matr)
                            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                            .setStartPoint("origin/master")
                            .setForce(false)
                            .call();
                } catch(GitAPIException ex){
                    //ex.printStackTrace();
                    System.out.println("Trabajando en branch existente");
                }

                //Push a remote
                try {
                    PushCommand pushCommand = git.push();
                    pushCommand.setRemote("origin");
                    pushCommand.setRefSpecs( new RefSpec(StartUpActions.matr) );
                    pushCommand.setCredentialsProvider(credentials);
                    pushCommand.call();
                    git.close();
                } catch (GitAPIException ex){
                    ex.printStackTrace();
                }
            }
        });
    }
}