package pluginPackage;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowId;
import java.io.File;

//Evento de click en nuevo botón
public class UpdateWorkspaceFile extends AnAction {
    public void actionPerformed(AnActionEvent e) {

    }

    //Mostrando el ícono en ventana de proyectos
    public void update(AnActionEvent e) {
        //Presentation presentation = e.getPresentation();
        Project project = (Project)e.getData(CommonDataKeys.PROJECT);
        try {
            ConfigureSettings workspaceFile = new ConfigureSettings(project);
            File outputFile = new File(project.getBasePath() + workspaceFile.consoleOutputPath);
            workspaceFile.readXML(project);
        }   catch (Exception er) {
            //er.printStackTrace();
        }
    }
}