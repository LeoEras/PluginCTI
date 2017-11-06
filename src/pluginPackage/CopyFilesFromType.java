package pluginPackage;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CopyFilesFromType
{
    public String[] foldersWithoutCopy = {".idea","out"};
    public DateFormat df = new SimpleDateFormat("dd,MM,yy,HH,mm,ss");
    public Date dateobj = new Date();

    public static void main(String[] args){
    }

    private FileTypeOrFolderFilter filter = null;

    public Date copy(final String fileType, String fromPath, String outputPath){
        filter = new FileTypeOrFolderFilter(fileType);
        File currentFolder = new File(fromPath);
        File outputFolder = new File(outputPath);
        String projectPath = fromPath.substring(0, fromPath.indexOf(currentFolder.getName()));
        dateobj = GlobalTime.getTimeFromServer();
        copyFilesFromFolders(projectPath, currentFolder.getAbsolutePath(), fileType, outputFolder);
        //scanFolder(fileType, currentFolder, outputFolder);
        //copyFilesFromFolders(fileType, currentFolder, outputFolder);
        return dateobj;
    }

    public void copyFilesFromFolders( String projectPath, String path, String fileType, File outputFolder) {

        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;

        for ( File f : list ) {
            if (!compareItems(f.getName(), foldersWithoutCopy)) {
                if (f.isDirectory()) {
                    copyFilesFromFolders(projectPath, f.getAbsolutePath(), fileType, outputFolder);
                } else {
                    if(f.getAbsoluteFile().getName().endsWith("." + fileType)) {
                        String projectAbsolutePath = projectPath.toLowerCase().replace("_","").replace("\\","_").replace("/","_").replaceAll("[.:!?(){}\\\\-]","");
                        String actualPath = f.getParent().toLowerCase().replace("_","").replace("\\","_").replace("/","_").replaceAll("[.:!?(){}\\\\-]","");
                        String source = actualPath.replace(projectAbsolutePath,"");
                        copy(f, source, outputFolder);
                    }
                }
            }
        }
    }

    public boolean compareItems(String a, String[] b) {
        boolean bandera = false;
        for (int i = 0; i < b.length; i++) {
            if (b[i].equalsIgnoreCase(a))
                bandera = true;
        }
        return bandera;
    }

    private void scanFolder(final String fileType, File currentFolder, File outputFolder){
        File[] files = currentFolder.listFiles(filter);
        for (File file : files) {
            if (!file.isDirectory()) {
                copy(file, currentFolder.getName(), outputFolder);
            }
        }
    }

    private void copy(File file, String currentFolderName, File outputFolder)
    {
        try {
            InputStream input = new FileInputStream(file);
            OutputStream out = new FileOutputStream(new File(outputFolder + File.separator +
                    currentFolderName + "_date" + df.format(dateobj) + "_" + Depurate(file.getName()) + ".py"));
            byte data[] = new byte[input.available()];
            input.read(data);
            out.write(data);
            out.flush();
            out.close();
            input.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String Depurate (String nameWithExtension){
        String nameWithoutExtension = "";
        int pos = nameWithExtension.lastIndexOf(".");
        //System.out.println(nameWithExtension);
        //System.out.println(pos);
        if (pos > 0) {
            nameWithoutExtension = nameWithExtension.substring(0, pos);
        }
        //System.out.println(nameWithoutExtension);
        return nameWithoutExtension;
    }

    private final class FileTypeOrFolderFilter implements FileFilter
    {
        private final String fileType;

        private FileTypeOrFolderFilter(String fileType)
        {
            this.fileType = fileType;
        }

        public boolean accept(File pathname)
        {
            return pathname.getName().endsWith("." + fileType) || pathname.isDirectory();
        }
    }
}
