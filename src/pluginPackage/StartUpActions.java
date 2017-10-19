package pluginPackage;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

//StartupActivity se ejecuta al abrir un proyecto.
public class StartUpActions implements StartupActivity {
    //static String npath = "";
    static String matricula = "";

    @Override
    public void runActivity(Project project) {
        //System.out.println(project.getBasePath());
        String remotePath = "http://est_espol@200.10.150.91/est_espol/Fundamentos.git";
        //String path = project.getBasePath() + "/.idea";
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider( "est_espol", "gPw19KX3_" );
        Repository localRepo = null;
        Git git;

        //Pidiendo la matrícula del estudiante

        String matricula = JOptionPane.showInputDialog("Ingrese su número de matrícula o cédula (9 dígitos): ");
        //String matricula = "";
        /*try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    CustomDialog dialog = new CustomDialog();
                    dialog.setVisible(true);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        /*
        final Runnable doCallCustomDialog = new Runnable() {
            public void run() {
                CustomDialog dialog = new CustomDialog();
                dialog.setVisible(true);
            }
        };

        Thread appThread = new Thread() {
            public void run() {
                try {
                    SwingUtilities.invokeAndWait(doCallCustomDialog);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("Finished on " + Thread.currentThread());
            }
        };
        appThread.start();
        //while(matr == ""){}
        */

        if (matricula == null){
            matricula = "";
        }
        while((matricula.length() != 9) || !matricula.matches("\\d*")){
            matricula = JOptionPane.showInputDialog("Ingrese su número de matrícula o cédula (9 dígitos): ");

            if (matricula == null){
                matricula = "";
            }
        }

        try {
            StartUpActions.matricula = matricula;

            //Configura Workspace
            ConfigureSettings workspaceFile = new ConfigureSettings(project);
            File outputFile = new File(project.getBasePath() + workspaceFile.consoleOutputPath);
            workspaceFile.deleteCreateFile(outputFile);
            workspaceFile.readXML(project);

            //Crea un nuevo directorio, empleando el ID de usuario, en la carpeta .idea
            //Luego crea un directorio por cada proyecto del usuario
            //String newpath = path + "\\" + matricula;
            //System.out.println(workspaceFile.folderPath);
            new File(workspaceFile.folderPath).mkdirs();

            //Crea referencia del repositorio local
            try {
                localRepo = new FileRepository(workspaceFile.folderPath + "/.git");
            } catch (java.io.IOException err){
                err.printStackTrace();
            }
            git = new Git(localRepo);

            //Clona el repositorio base (master) en la nueva carpeta, o intenta si esta ya existe
            try {
                CloneCommand cloneCommand = Git.cloneRepository();
                cloneCommand.setURI(remotePath);
                cloneCommand.setCredentialsProvider(credentials);
                cloneCommand.setDirectory(new File(workspaceFile.folderPath)).call();
            } catch (JGitInternalException | GitAPIException ex){
                //La carpeta ya existe y tiene un .git asociado
                //ex.printStackTrace();
                System.out.println("Esta carpeta ya contiene un .git asociado");
            }

            //Realiza pull del branch perteneciente al ID de usuario, por si en remote ya tiene algo que en esta
            //máquina no tenga
            try {
                git.pull().setRemoteBranchName(matricula).setCredentialsProvider(credentials).call();
            } catch (RefNotAdvertisedException ex){
                //ex.printStackTrace();
                System.out.println("Remote " + matricula + " no existe");
            }
            //Crea archivo para registrar entradas del usuario a sus proyectos
            File users = new File(workspaceFile.userPath);
            users.createNewFile();

            DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
            Date dateobj = new Date();
            FileWriter fileWriter = new FileWriter(workspaceFile.userPath, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(df.format(dateobj) + " " + project.getName() +"\n");
            printWriter.close();

            //Crea un nuevo directorio, empleando el ID de usuario para los archivos del proyecto
            //newpath = newpath + "\\" + project.getName();
            new File(workspaceFile.projectPath).mkdirs();

        } catch (IOException | GitAPIException ex) {
            ex.printStackTrace();
        }
    }

}