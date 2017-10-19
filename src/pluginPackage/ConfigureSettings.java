package pluginPackage;

import com.intellij.openapi.project.Project;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import java.io.File;
import java.io.IOException;

public class ConfigureSettings {
    public String consoleOutputFile = "output.txt";
    public String consoleOutputPath = "";
    public String workspacePath = "";
    public String logPath = "";
    public String userPath = "";
    public String folderPath = "";
    public String projectPath = "";
    public static String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    public static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }

    public ConfigureSettings(Project project){
        setOutputPath(project);
    }

    public void setOutputPath(Project project){
        if (isWindows()) {
            //System.out.println("Es un Windows");
            folderPath = project.getBasePath() + "/.idea/" + StartUpActions.matricula;
            projectPath = folderPath + "\\" + project.getName();
            consoleOutputPath = "/.idea/" + consoleOutputFile;
            workspacePath = "\\.idea\\workspace.xml";
            logPath = projectPath + "\\" + "log.log";
            userPath = folderPath + "\\" + "user.log";
        } else if (isMac()) {
            //System.out.println("Es un Mac");
            folderPath = project.getBasePath() + "/.idea/" + StartUpActions.matricula;
            projectPath = folderPath + "\\" + project.getName();
            consoleOutputPath = "/.idea/" + consoleOutputFile;
            workspacePath = "\\.idea\\workspace.xml";
            logPath = projectPath + "\\" + "log.log";
            userPath = folderPath + "\\" + "user.log";
        }else{
            //System.out.println("Es un Unix/Linux");
            folderPath = project.getBasePath() + "/.idea/" + StartUpActions.matricula;
            projectPath = folderPath + "\\" + project.getName();
            consoleOutputPath = "/.idea/" + consoleOutputFile;
            workspacePath = "\\.idea\\workspace.xml";
            logPath = projectPath + "\\" + "log.log";
            userPath = folderPath + "\\" + "user.log";
        }
    }

    public void createFile(File newFile){
        if(!newFile.exists()){
            try {
                newFile.createNewFile();
                //System.out.println("File:" + newFile + " was created.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void deleteCreateFile(File newFile){
        if(newFile.exists()){
            if(newFile.delete()){
                //System.out.println("File:" + newFile + " was deleted.");
                createFile(newFile);
            }else{
                System.out.println("Delete operation has failed.");
            }
        }
        else{
            createFile(newFile);
        }
    }

    public void readXML(Project project) {
        try {
            String ruta = project.getBasePath();
            String xmlFile = ruta + workspacePath; //"\\.idea\\workspace.xml";
            Boolean findNode = false;

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();
            //System.out.println("\nRoot element :" + doc.getDocumentElement().getNodeName());
            NodeList nList = doc.getElementsByTagName("component");
            //System.out.println("----------------------------");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                //System.out.println("\nCurrent Element :" + nNode.getNodeName());

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String nameComponent = eElement.getAttribute("name");
                    if(nameComponent.equalsIgnoreCase("RunManager")) {
                        findNode = true;
                        NodeList nListConfig = eElement.getElementsByTagName("configuration");
                        Node nNodeConfig = nListConfig.item(0);
                        //System.out.println("\nCurrent Element :" + nNodeConfig.getNodeName());
                        if (nNodeConfig.getNodeType() == Node.ELEMENT_NODE) {
                            Element eOutputFile = (Element) nNodeConfig;
                            //System.out.println("Length Element Tag:" + eOutputFile.getElementsByTagName("output_file").getLength());
                            if(eOutputFile.getElementsByTagName("output_file").getLength() != 0) {
                                NodeList nOutputFile = eOutputFile.getElementsByTagName("output_file");
                                //System.out.println("Update Element :" + nOutputFile.getLength());
                                Node nNodeOutput = nOutputFile.item(0);
                                Element eNewOutputFile = (Element) nNodeOutput;
                                eNewOutputFile.setAttribute("path", "$PROJECT_DIR$/.idea/" + consoleOutputFile);
                                eNewOutputFile.setAttribute("is_save", "true");
                            }
                            else{
                                Element eNewOutputFile = doc.createElement("output_file");
                                eNewOutputFile.setAttribute("path", "$PROJECT_DIR$/.idea/" + consoleOutputFile);
                                eNewOutputFile.setAttribute("is_save", "true");
                                //System.out.println("Create Element output_file");
                                nNodeConfig.insertBefore(eNewOutputFile, nNodeConfig.getFirstChild());
                            }
                        }
                        else {
                            //crear el nodo
                            //System.out.println("Crear nodo configuration");
                            Element eNewConfiguration = doc.createElement("configuration");
                            doc.appendChild(eNewConfiguration);
                            Element eNewOutputFile = doc.createElement("output_file");
                            eNewOutputFile.setAttribute("path", "$PROJECT_DIR$/.idea/" + consoleOutputFile);
                            eNewOutputFile.setAttribute("is_save", "true");
                            eNewConfiguration.appendChild(eNewOutputFile);
                        }

                    }
                }
            }
            //System.out.println("\nVariable:" + findNode);
            if(findNode.equals(false)){
                //crear el nodo
                NodeList nProject = doc.getElementsByTagName("project");
                Node nNodeProject = nProject.item(0);

                if (nNodeProject.getNodeType() == Node.ELEMENT_NODE) {
                    Element eNewComponent = doc.createElement("component");
                    eNewComponent.setAttribute("name", "RunManager");
                    nNodeProject.insertBefore(eNewComponent, nNodeProject.getFirstChild());
                    //System.out.println("Crear nodo component");
                    Element eNewConfiguration = doc.createElement("configuration");
                    eNewComponent.appendChild(eNewConfiguration);
                    Element eNewOutputFile = doc.createElement("output_file");
                    eNewOutputFile.setAttribute("path", "$PROJECT_DIR$/.idea/" + consoleOutputFile);
                    eNewOutputFile.setAttribute("is_save", "true");
                    eNewConfiguration.appendChild(eNewOutputFile);
                }
            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(xmlFile));
            transformer.transform(source, result);
            //System.out.println("Done!");
            //System.out.println(doc);

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }catch (SAXException sae) {
            sae.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void setAttributeOutputElement(Element eNewOutputFile){
        eNewOutputFile.setAttribute("path", "$PROJECT_DIR$/.idea/" + consoleOutputFile);
        eNewOutputFile.setAttribute("is_save", "true");
        System.out.println("Path: " + eNewOutputFile.getAttribute("path"));
    }

    public static void main(String argv[]) {

    }

}