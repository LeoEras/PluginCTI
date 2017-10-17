package pluginPackage;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.actions.ChooseRunConfigurationPopup;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowId;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.BufferedReader;
import java.io.FileReader;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

//Evento de click en nuevo botón
public class NewPlayButtonActions extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        Project project = (Project)e.getData(CommonDataKeys.PROJECT);
        String path = StartUpActions.npath;
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider( "est_espol", "gPw19KX3_" );
        String logpath = path + "\\" + project.getName() + "\\" + "log.log";

        //Configure workspace file, add output file
        ConfigureWorkspaceFile workspaceFile = new ConfigureWorkspaceFile();
        File outputFile = new File(project.getBasePath() + workspaceFile.consoleOutputPath);
        workspaceFile.createFile(outputFile);
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

        //Genera ventana para realizar ejecución del script
        ChooseRunConfigurationPopup popup = new ChooseRunConfigurationPopup(project, this.getAdKey(), this.getDefaultExecutor(), this.getAlternativeExecutor());
        popup.show();
        System.out.println(popup.getExecutor());

        try {
            File log = new File(logpath);
            BufferedReader br = null;
            FileReader fr = null;

            //Punteros para escribir en archivo log.log, para no sobreescribir se pone append: true
            FileWriter fileWriter = new FileWriter(logpath, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);

            System.out.println(df.format(dateobj));

            if (log.createNewFile()){
                printWriter.print(df.format(dateobj) + "\t");
            } else {
                printWriter.print("\n" + df.format(dateobj) + "\t");
                fr = new FileReader(project.getBasePath() + workspaceFile.consoleOutputPath);
                br = new BufferedReader(fr);

                String sCurrentLine;

                while ((sCurrentLine = br.readLine()) != null) {
                    printWriter.print(sCurrentLine);
                }

                printWriter.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        //Copiando el archivo en proceso de ejecución al repositorio, para análisis
        /*File source = new File(project.getBasePath());
        File dest = new File(path + "\\" + project.getName());
        try {
            FileUtils.copyDirectory(source, dest);
        } catch (IOException error) {
            error.printStackTrace();
        }*/

        new CopyFilesFromType().copy("py", project.getBasePath(), path + "\\" + project.getName());

        //Git checkout indica al branch al cual se realizará el push
        try {
            git.checkout().setName(StartUpActions.matr).call();
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Nuevo commit, fecha: " + dateobj.toString()).call();
        } catch (Exception ex){
            ex.printStackTrace();
        }

        //Por si no existe en remote el branch, se debe de crear. En caso de existir, imprime el error
        try {
            CreateBranchCommand bcc = git.branchCreate();
            bcc.setName(StartUpActions.matr)
                    .setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
                    .setStartPoint("origin/master")
                    .setForce(false)
                    .call();
        } catch(GitAPIException ex){
            ex.printStackTrace();
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

    //Muestra ventana de Run...
    protected Executor getDefaultExecutor() {
        return DefaultRunExecutor.getRunExecutorInstance();
    }

    //Por si no funciona la ventana de Run... despliega la de Debug...
    protected Executor getAlternativeExecutor() {
        return ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG);
    }

    protected String getAdKey() {
        return "run.configuration.alternate.action.ad";
    }

    //Mostrando el ícono en ventana de proyectos
    public void update(AnActionEvent e) {

        Presentation presentation = e.getPresentation();
        Project project = (Project)e.getData(CommonDataKeys.PROJECT);
        try {
            ConfigureWorkspaceFile workspaceFile = new ConfigureWorkspaceFile();
            File outputFile = new File(project.getBasePath() + workspaceFile.consoleOutputPath);
            workspaceFile.createFile(outputFile);
            workspaceFile.readXML(project);
        }   catch (Exception er) {
            er.printStackTrace();
        }


        presentation.setEnabled(true);
        if (project != null && !project.isDisposed()) {
            if (null == this.getDefaultExecutor()) {
                presentation.setEnabled(false);
                presentation.setVisible(false);
            } else {
                presentation.setEnabled(true);
                presentation.setVisible(true);
            }
        } else {
            presentation.setEnabled(false);
            presentation.setVisible(false);
        }
    }

    //public boolean isDumbAware() {
    //    return Registry.is("dumb.aware.run.configurations");
    //}
}