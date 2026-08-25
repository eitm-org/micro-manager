package org.micromanager.saveafproperties;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.micromanager.AutofocusPlugin;
import org.micromanager.Studio;
import org.micromanager.internal.utils.PropertyItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SaveAFPropertiesFrame extends JDialog {

   private final Studio studio_;

   private JLabel filePathLabel_;
   private JLabel fileLabel_;

   private JButton loadButton_;
   private JButton saveButton_;


   public SaveAFPropertiesFrame(Studio studio) {

      studio_ = studio;

      setTitle("Save AF Properties");

      setSize(600, 150);

      setLocationRelativeTo(null);

      createUserInterface();
   }


   private void createUserInterface() {

      setLayout(new BorderLayout());

      JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

      fileLabel_ = new JLabel("File:");
      filePathLabel_ = new JLabel("No file selected");

      filePanel.add(fileLabel_);
      filePanel.add(filePathLabel_);

      JPanel buttonPanel = new JPanel(new FlowLayout());

      loadButton_ = new JButton("Load");

      saveButton_ = new JButton("Save");

      buttonPanel.add(loadButton_);
      buttonPanel.add(saveButton_);

      add(filePanel, BorderLayout.CENTER);
      add(buttonPanel, BorderLayout.SOUTH);

      loadButton_.addActionListener(e -> loadFile());

      saveButton_.addActionListener(e -> saveFile());
   }


   // load file
    private void loadFile() {

        JFileChooser fileChooser = new JFileChooser();

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {

            File file = fileChooser.getSelectedFile();

            filePathLabel_.setText(file.getAbsolutePath());
            fileLabel_.setText("Settings loaded from:");

            loadAFProperties(file);
        }
    }

    private void loadAFProperties(File file) {

    try {
        AutofocusPlugin autofocus = studio_.getAutofocusManager() .getAutofocusMethod();

        if (autofocus == null) {

            JOptionPane.showMessageDialog(
                this,
                "No autofocus plugin is currently selected.",
                "Load AF Properties",
                JOptionPane.ERROR_MESSAGE);

            return;
        }

        // read xml file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(file);

        Element root = document.getDocumentElement();

        if (!"autofocus".equals(root.getTagName())) {

            throw new Exception("Invalid XML file. Expected <autofocus>.");
        }


        // load each property

        NodeList propertyNodes =
                root.getElementsByTagName("property");

        String currentMethodName = autofocus.getName();
        studio_.getAutofocusManager().refresh();

        for (int i = 0;
            i < propertyNodes.getLength();
            i++) {

            Element propertyElement = (Element) propertyNodes.item(i);

            String name = propertyElement.getAttribute("name");

            String value = propertyElement.getAttribute("value");

            if (name == null || name.isEmpty()) {
                continue;
            }

            System.out.println("Loading: " + name + " = " + value);

            autofocus.setPropertyValue(
                name,
                value);
        }

        // Update AF plugin's internal variables, refresh manager and reselect current method
        autofocus.applySettings();
        autofocus.saveSettings();
        studio_.getAutofocusManager().refresh();
        studio_.getAutofocusManager().setAutofocusMethodByName(currentMethodName);
        studio_.app().refreshGUI();

        System.out.println( "Properties after loading:");

        for (PropertyItem property : autofocus.getProperties()) {

            System.out.println( "  " + property.name + " = " + property.value);
        }


        JOptionPane.showMessageDialog( this, "AF properties loaded successfully.", "Load AF Properties", JOptionPane.INFORMATION_MESSAGE);


    } catch (Exception e) {

        JOptionPane.showMessageDialog( this, "Error loading AF properties:\n" + e.getMessage(), "Load AF Properties", JOptionPane.ERROR_MESSAGE);

        e.printStackTrace();
    }
    }

   // save file

   private void saveFile() {
      JFileChooser fileChooser = new JFileChooser();
      
      String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      AutofocusPlugin autofocus = studio_.getAutofocusManager().getAutofocusMethod();

      String pluginName = "Autofocus";

      if (autofocus != null) {
        pluginName = autofocus.getName();
      }

      pluginName = pluginName.replaceAll("[\\\\/:*?\"<>|]", "_");

      String defaultFileName = date + "_" + pluginName + "_Parameters.xml";
      
      fileChooser.setSelectedFile(new File(defaultFileName));
      
      int result =fileChooser.showSaveDialog(this);

      if (result == JFileChooser.APPROVE_OPTION) {

        File file = fileChooser.getSelectedFile();

        if (!file.getName() .toLowerCase() .endsWith(".xml")) {
           file = new File( file.getAbsolutePath() + ".xml");
        }

        // Handle the case where the file already exists
        String fullPath = file.getAbsolutePath();
        String basePath = fullPath;
        String extension = "";

        // Strip and save file extension (xml)
        int dotIndex = fullPath.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex > fullPath.lastIndexOf(File.separatorChar)) {
            basePath = fullPath.substring(0, dotIndex);
            extension = fullPath.substring(dotIndex);
        }

        // Add counter to base path
        int counter = 2;
        while (file.exists()) {
            file = new File(basePath + "_" + counter + extension);
            counter++;
        }

        filePathLabel_.setText(file.getAbsolutePath());
        fileLabel_.setText("Settings saved as:");

        saveAFProperties(file);
      }
   }


   private void saveAFProperties(File file) {

      try {
         AutofocusPlugin autofocus = studio_.getAutofocusManager() .getAutofocusMethod();

         if (autofocus == null) {

            JOptionPane.showMessageDialog(
                  this,
                  "No autofocus plugin is currently selected.",
                  "Save AF Properties",
                  JOptionPane.ERROR_MESSAGE);

            return;
         }


         // get af properties

         PropertyItem[] properties = autofocus.getProperties();

         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

         DocumentBuilder builder = factory.newDocumentBuilder();

         Document document = builder.newDocument();

         Element autofocusElement = document.createElement("autofocus");
 
         autofocusElement.setAttribute( "plugin", autofocus.getName());

         document.appendChild(autofocusElement);


         //add properties

         for (PropertyItem property : properties) {

            Element propertyElement = document.createElement("property");

            propertyElement.setAttribute( "name", property.name);

            propertyElement.setAttribute( "value", property.value);

            autofocusElement.appendChild(propertyElement);
         }


         TransformerFactory transformerFactory =  TransformerFactory.newInstance();

         Transformer transformer =  transformerFactory.newTransformer();


         transformer.setOutputProperty(
               OutputKeys.INDENT,
               "yes");


         transformer.setOutputProperty(
               OutputKeys.ENCODING,
               "UTF-8");


         // write xml

         DOMSource source = new DOMSource(document);

         StreamResult output = new StreamResult(file);

         transformer.transform(source, output);


         JOptionPane.showMessageDialog(
               this,
               "AF properties saved successfully.",
               "Save AF Properties",
               JOptionPane.INFORMATION_MESSAGE);


      } catch (Exception e) {

         JOptionPane.showMessageDialog(
               this,
               "Error saving AF properties:\n"
                     + e.getMessage(),
               "Save AF Properties",
               JOptionPane.ERROR_MESSAGE);

         e.printStackTrace();
      }
   }
}