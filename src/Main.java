import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;

public class Main {
    private String outputFile = "";
    public static void main(String[] args)  {
        createAndShowGUI();
    }
    public static void createAndShowGUI() {

        JFrame frame = new JFrame("XML Search App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();

        JLabel xmlLabel = new JLabel("XML File Path:");
        JTextField xmlTextField = new JTextField(20);

        JButton chooseFileButton = new JButton("Choose XML File Location");
        chooseFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();

            String initialPath = "D:\\Projects\\jguar_GIT_Set";
            File initialDirectory = new File(initialPath);
            if(initialDirectory.exists() && initialDirectory.isDirectory()){
                fileChooser.setCurrentDirectory(initialDirectory);
            }

            int returnValue = fileChooser.showOpenDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String filePath = selectedFile.getAbsolutePath();

                // Check if the selected file is an XML file
                if (filePath.toLowerCase().endsWith(".xml")) {
                    xmlTextField.setText(filePath);
                } else {
                    JOptionPane.showMessageDialog(frame, "Selected file is not an XML file.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // create an object of font type
        Font fo = new Font("Serif", Font.PLAIN, 16);

        // set the font of the textfield
        xmlTextField.setFont(fo);

        JButton chooseOutputFileButton = new JButton("Choose Output File Location");

        Main test = new Main();
        chooseOutputFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showSaveDialog(frame);

            if (option == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                test.outputFile = selectedFile.getAbsolutePath();
                if(!(selectedFile.getAbsolutePath().endsWith(".txt"))){
                    test.outputFile += ".txt";
                }
                System.out.println("Selected File: " + selectedFile.getAbsolutePath());
            }
        });


        JButton runButton = new JButton("Run");

        runButton.addActionListener(e -> {
            String xmlFilePath = xmlTextField.getText();
            String outputFileName = test.outputFile;

            if(!xmlFilePath.endsWith(".xml")){
                JOptionPane.showMessageDialog(frame, "Selected file is not an XML file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if(outputFileName.isEmpty()){
                JOptionPane.showMessageDialog(frame, "You should select output file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
               search(xmlFilePath,outputFileName);
            } catch (IOException | SAXException ex) {
                throw new RuntimeException(ex);
            }

            JOptionPane.showConfirmDialog(frame, "Search completed and output saved.", "Message", JOptionPane.DEFAULT_OPTION);
        });


        panel.add(xmlLabel);
        panel.add(xmlTextField);
        panel.add(chooseFileButton);
        panel.add(chooseOutputFileButton);
        panel.add(runButton);

        frame.add(panel);
        frame.setSize(600,300);
        frame.setResizable(false);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (screenSize.width - frame.getWidth()) / 2;
        int centerY = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(centerX, centerY);

        frame.setVisible(true);
    }
    public static void search(String xmlFilePath,String outputFileName) throws IOException, SAXException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(new File(xmlFilePath));

            document.getDocumentElement().normalize();

            //Element alınır
            NodeList links = document.getElementsByTagName("link");
            if(links.getLength() == 0){
                JOptionPane.showMessageDialog(null, "There is no link tag here.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Map<String, java.util.List<Element>> aliasMap = new HashMap<>();
            java.util.List<String> notRepeatedAlias = new ArrayList<>();


            PrintWriter writer = new PrintWriter( outputFileName);


            for(int i = 0;i<links.getLength();i++){
                Node linkNode = links.item(i);

                if(linkNode.getNodeType() == Node.ELEMENT_NODE){
                    Element linkElement = (Element) linkNode;
                    String alias = linkElement.getAttribute("alias");
                    Element parentTable = (Element) linkElement.getParentNode().getParentNode();

                    if(!notRepeatedAlias.contains(alias)){
                        notRepeatedAlias.add(alias);
                    }

                    aliasMap.computeIfAbsent(alias, k -> new ArrayList<>()).add(parentTable);
                }
            }

            //tekrar eden aliasları dönen map
            Map<String,Integer> itemCounts = repeatedAlias(notRepeatedAlias);

            //aliasMap ile karşılaştır,tekrar eden değerleri tablosu ile bulabilmek için
            Map<String, List<Element>> newAliasMap = compareAndRemove(itemCounts,aliasMap);

            for (Map.Entry<String, List<Element>> entry : newAliasMap.entrySet()) {
                writer.write("Duplicate Alias: " + entry.getKey() + "\n");
                for (Element parentTable : entry.getValue()) {
                    String table = parentTable.getAttribute("name");
                    writer.write("  - Linked Table : " + table + "\n");
                }
                writer.write("\n");

            }

            writer.close();

        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

    }

    public static Map<String,Integer> repeatedAlias(java.util.List<String> notRepeatedAlias) {

        notRepeatedAlias.replaceAll(String::toLowerCase);
        Map<String, Integer> itemCounts = new HashMap<>();

        // Alias sayısını hesaplama
        for (String item : notRepeatedAlias) {
            itemCounts.put(item, itemCounts.getOrDefault(item, 0) + 1);
        }

        Iterator<Map.Entry<String, Integer>> iterator = itemCounts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            if (!(entry.getValue() > 1)) {
                iterator.remove();
            }
        }

        return itemCounts;
    }

    public static Map<String, List<Element>> compareAndRemove(Map<String, Integer> itemCounts, Map<String, java.util.List<Element>> aliasMap) {
        Iterator<Map.Entry<String, List<Element>>> iterator = aliasMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, List<Element>> entry = iterator.next();
            String lowercaseKey = entry.getKey().toLowerCase();
            if (!itemCounts.containsKey(lowercaseKey)) {
                iterator.remove();
            }
        }
        return  removeDuplicatesExceptOne(aliasMap);
    }

    public static Map<String, List<Element>> removeDuplicatesExceptOne(Map<String, List<Element>> map) {
        Map<String, List<Element>> tempMap = new HashMap<>();

        for (Map.Entry<String, List<Element>> entry : map.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (!(tempMap.containsKey(key))) {
                tempMap.put(key, new ArrayList<>(entry.getValue()));
            } else {
                List<Element> existingValue = tempMap.get(key);
                existingValue.addAll(entry.getValue());
                tempMap.put(key, existingValue);
            }

        }
        return tempMap;
    }
}
