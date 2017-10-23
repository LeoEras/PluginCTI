package pluginPackage;

import org.codehaus.plexus.util.FileUtils;

import javax.swing.*;
import javax.swing.text.*;
import java.io.File;
import java.io.IOException;

public class SizeFilter extends JPanel{
    //JTextField textfield = new JTextField(10);

    public SizeFilter(JTextField textfield, Integer length) {
        PlainDocument doc = (PlainDocument) textfield.getDocument();
        doc.setDocumentFilter(new TextLengthDocFilter(length));

        add(textfield);
    }

    private class TextLengthDocFilter extends DocumentFilter {
        private int maxTextLength;

        public TextLengthDocFilter(int maxTextLength) {
            this.maxTextLength = maxTextLength;
        }

        private boolean verifyText(String text) {
            return text.length() <= maxTextLength;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string,
                                 AttributeSet attr) throws BadLocationException {

            Document doc = fb.getDocument();
            String oldText = doc.getText(0, doc.getLength());
            StringBuilder sb = new StringBuilder(oldText);
            sb.insert(offset, string);

            if (verifyText(sb.toString())) {
                super.insertString(fb, offset, string, attr);
            }

        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            Document doc = fb.getDocument();
            String oldText = doc.getText(0, doc.getLength());
            StringBuilder sb = new StringBuilder(oldText);

            sb.replace(offset, offset + length, text);
            if (verifyText(sb.toString())) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            Document doc = fb.getDocument();
            String oldText = doc.getText(0, doc.getLength());
            StringBuilder sb = new StringBuilder(oldText);

            sb.replace(offset, offset + length, "");

            if (verifyText(sb.toString())) {
                super.remove(fb, offset, length);
            }
        }
    }

    public static void main(String[] args) {
        File source = new File("C:\\Users\\Norman-Laptop\\IdeaProjects\\test\\.idea\\000000025");
        File dest = new File("C:\\Users\\Norman-Laptop\\IdeaProjects\\test\\.idea\\000000025_TMP");
        try {
            FileUtils.copyDirectoryStructure(source, dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}