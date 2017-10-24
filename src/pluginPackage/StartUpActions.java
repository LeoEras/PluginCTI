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
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

//StartupActivity se ejecuta al abrir un proyecto.
public class StartUpActions implements StartupActivity {
    static String matricula = "";
    private boolean conexion = true;
    static boolean contieneGit = false;

    @Override
    public void runActivity(Project project) {
        String remotePath = "http://est_espol@200.10.150.91/est_espol/Fundamentos.git";

        //Credenciales para autenticacion con gitlab.
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider( "est_espol", "gPw19KX3_" );
        Repository localRepo = null;
        Git git;

        //Pidiendo la matrícula del estudiante.
        String matricula = JOptionPane.showInputDialog("Ingrese su número de matrícula o cédula (9 dígitos): ");
        if (matricula == null){
            matricula = "";
        }
        while((matricula.length() != 9) || !matricula.matches("\\d*")){
            matricula = JOptionPane.showInputDialog("Ingrese su número de matrícula o cédula (9 dígitos): ");

            if (matricula == null){
                matricula = "";
            }
        }

        StartUpActions.matricula = matricula;

        //Configura variables que manejan archivos y carpetas del proyecto, ademas del manejo del output.
        ConfigureSettings workspaceFile = new ConfigureSettings(project);
        File outputFile = new File(project.getBasePath() + workspaceFile.consoleOutputPath);
        workspaceFile.deleteCreateFile(outputFile);
        workspaceFile.readXML(project);

        //Crea un nuevo directorio, empleando el ID de usuario, en la carpeta .idea.
        //Luego crea un directorio para el proyecto actual del usuario.
        new File(workspaceFile.folderPath).mkdirs();

        //Crea referencia del repositorio local.
        try {
            localRepo = new FileRepository(workspaceFile.folderPath + "/.git");
        } catch (java.io.IOException err){
            err.printStackTrace();
        }
        git = new Git(localRepo);

        //Prueba de conexion con servidor.
        try{
            LsRemoteCommand lscommand = git.lsRemote();
            lscommand.setRemote(remotePath).setCredentialsProvider(credentials).call();
        } catch (TransportException tex){
            conexion = false;
            System.out.println("No hay conexion");
        } catch (GitAPIException ex){
            System.out.println("Error general con JGit.");
        }

        //Clona el repositorio base (master) en la nueva carpeta en caso de haber conexion, y revisa si ya existe un .git
        //asociado.
        try {
            CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setURI(remotePath);
            cloneCommand.setCredentialsProvider(credentials);
            cloneCommand.setDirectory(new File(workspaceFile.folderPath)).call();
            contieneGit = true;
        } catch (TransportException tr){
            conexion = false;
            System.out.println("No hay conexion");
        } catch (JGitInternalException ie){
            System.out.println("Esta carpeta ya contiene un .git asociado");
            /*Cuando se tiene un .git asociado con la carpeta, JGit ignora 'TransportException'. Debido a la prueba de
            conexion anterior a la operacion 'clone', se tienen los siguientes casos:
                1. Hay conexion y existe un .git asociado a la carpeta: Se puede ignorar la operacion de 'clone.' Este
                   es el caso en que vuelva a trabajar el mismo estudiante con el mismo proyecto.
                2. No hay conexion y existe un .git asociado de forma previa: Ya que la operacion 'pull' es lenta, se
                   prefiere no realizarla. Es necesario ademas notificar a la clase 'NewActionsForPlayButton' que ya hay
                   un .git asociado. De esta forma se evita realizar la operacion 'clone' durante 'Run.'
                   No marcar esto genera conflictos, ya que es imposible borrar la carpeta .git mientras el programa este
                   abierto.
            */
            if(!conexion){
                contieneGit = true;
            }
        } catch (GitAPIException apiex){
            //apiex.printStackTrace();
            System.out.println("Error general de JGit.");
        }

        //Realiza pull del branch perteneciente al ID de usuario, por si 'remote' ya existe algo que 'local' no tenga.
        //Debido a que es una operacion lenta, se pregunta por la conexion antes de realizarla.
        if (conexion && contieneGit){
            try {
                git.pull().setRemoteBranchName(matricula).setCredentialsProvider(credentials).call();
            } catch (RefNotAdvertisedException a) {
                System.out.println("Remote " + matricula + " no existe");
            } catch (GitAPIException ex){
                System.out.println("Error general con JGit.");
            } finally {
                git.close();
            }
        }

        git.close();

        //Crea archivo para registrar entradas del usuario a sus proyectos
        try{
            File users = new File(workspaceFile.userPath);
            users.createNewFile();

            DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
            Date dateobj = new Date();
            FileWriter fileWriter = new FileWriter(workspaceFile.userPath, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(df.format(dateobj) + " " + project.getName() +"\n");
            printWriter.close();
        } catch (IOException ex){
            System.out.println("Error en manejo de archivos");
        }

        //Crea un nuevo directorio, empleando el ID de usuario para los archivos del proyecto:
        //newpath = newpath + "\\" + project.getName();
        new File(workspaceFile.projectPath).mkdirs();
    }

}