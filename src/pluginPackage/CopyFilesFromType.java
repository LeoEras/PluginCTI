package pluginPackage;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CopyFilesFromType
{
    public static void main(String[] args){
        /*new CopyFilesFromType().copy("py", "C:\\Users\\leonardo.eras\\IdeaProjects\\BBB",
                "C:\\Users\\leonardo.eras\\IdeaProjects\\BBB\\.idea\\000000000\\BBB");*/
    }

    private FileTypeOrFolderFilter filter = null;

    public void copy(final String fileType, String fromPath, String outputPath){
        filter = new FileTypeOrFolderFilter(fileType);
        File currentFolder = new File(fromPath);
        File outputFolder = new File(outputPath);
        scanFolder(fileType, currentFolder, outputFolder);
    }

    private void scanFolder(final String fileType, File currentFolder, File outputFolder){
        //System.out.println("Scanning folder [" + currentFolder + "]...");
        File[] files = currentFolder.listFiles(filter);
        for (File file : files) {
            /*if (file.isDirectory()) {
                //scanFolder(fileType, file, outputFolder);
            } else {
                copy(file, outputFolder);
            }*/
            if (!file.isDirectory()) {
                copy(file, outputFolder);
            }
        }
    }

    private void copy(File file, File outputFolder)
    {
        DateFormat df = new SimpleDateFormat("dd,MM,yy,HH,mm,ss");
        Date dateobj = new Date();
        try {
            //System.out.println("\tCopying [" + file + "] to folder [" + outputFolder + "]...");
            InputStream input = new FileInputStream(file);
            OutputStream out = new FileOutputStream(new File(outputFolder + File.separator +
                    df.format(dateobj) + "_" + Depurate(file.getName()) + ".py"));
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
