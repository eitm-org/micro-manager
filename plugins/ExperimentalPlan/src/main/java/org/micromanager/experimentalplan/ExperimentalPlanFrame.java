package org.micromanager.experimentalplan;

import org.micromanager.Studio;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Desktop;
import java.net.URI;

import java.io.File;
import java.util.List;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.IOException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.micromanager.acquisition.SequenceSettings;
import org.w3c.dom.events.MouseEvent;

public class ExperimentalPlanFrame extends JDialog{
    private final Studio studio_;

    private JButton applyButton_;
    private JButton refreshButton_;
    private JButton changeLinkButton_;
    private JLabel rootFolderLabel_;

    private final ExperimentParser experimentParser_;
    private JComboBox<String> experimentComboBox_;

    private static final String DROPBOX_LINK_KEY = "dropboxLink";
    private static final String DEFAULT_DROPBOX_LINK = "https://www.dropbox.com/scl/fi/ymre7hihjfkmns4l7nuv5/TCellAnalyzer_Experiments_Summary_Active_2026.xlsx?rlkey=68yqrxr1do312jq2lvyie0ag9&st=8o78hj4a&dl=1";

    private static final String EXCEL_FILE_PATH = "C:\\Users\\FionaRyan\\EMI Dropbox\\_Dept_Convergence_Systems_Research\\Project_Cell Analyzer Imaging Platform\\01_Planning\\TCellAnalyzer_Experiments_Summary_Active_2026.xlsx";

    private static final String ROOT_PARENT_FOLDER = "C:\\Users\\FionaRyan\\Experiments";
    private final Preferences preferences_;
    
    public ExperimentalPlanFrame(Studio studio) {

        studio_ = studio;

        experimentParser_ = new ExperimentParser();

        preferences_ = Preferences.userNodeForPackage(ExperimentalPlanFrame.class);

        setTitle("CAI Experiment ID");

        setSize(700, 300);

        setLocationRelativeTo(null);

        createUserInterface();
        loadExperiments();
    }


    private void createUserInterface() {

        JPanel mainPanel = new JPanel(new BorderLayout());

        // title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.add(new JLabel(
            "<html>"
            + "These Experiment IDs are marked with a status of 'Planned' in the master Excel file <br> "
            + "located in the 'Project_Cell Analyzer Imaging Platform' Dropbox folder.<br>"
            + "Select an experiment in the dropdown and click 'Apply' to set the root folder for your experiment."
            + "</html>"
        ));

        // controls
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10,10, 10);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;

        controlsPanel.add(new JLabel("Planned experiments:"),gbc);

        // dropdown for experiment IDs
        experimentComboBox_ = new JComboBox<>();

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        controlsPanel.add(experimentComboBox_, gbc);

        applyButton_ = new JButton("Apply");

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridheight = 1;

        // apply button to change root folder
        controlsPanel.add(applyButton_, gbc);
        applyButton_.addActionListener(e -> applyExperiment());

        refreshButton_ = new JButton("<html>Refresh list of<br>Experiment IDs</html>");
        refreshButton_.addActionListener(e -> refreshExperiments());

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridheight = 1;

        // refresh button to reload the list of experiment IDs
        controlsPanel.add(refreshButton_, gbc);

        JButton openDropboxButton = new JButton("Open Excel file");
        openDropboxButton.addActionListener(e -> openDropboxFile());
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;

        controlsPanel.add(openDropboxButton, gbc);

        changeLinkButton_ = new JButton("<html>Change link<br>to dropbox</html>");
        changeLinkButton_.addActionListener(e -> changeDropboxLink());

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 1;

        // change link button to change the dropbox link
        controlsPanel.add(changeLinkButton_, gbc);      


        // bottom text to display root folder
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(new JLabel("Root folder will become: "));
        rootFolderLabel_ = new JLabel("");
        bottomPanel.add(rootFolderLabel_);

        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(controlsPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    // Refresh button - refresh all the experiment IDs from the Excel file in the dropdown
    private void refreshExperiments() {

        try {
            // get the saved Dropbox link
            String dropboxLink = preferences_.get( DROPBOX_LINK_KEY, DEFAULT_DROPBOX_LINK );

            // make sure a Dropbox link has been configured
            if (dropboxLink.equals( "PASTE_YOUR_DROPBOX_LINK_HERE")) {
                JOptionPane.showMessageDialog( this, "No Dropbox link has been configured yet.", "Dropbox Link", JOptionPane.INFORMATION_MESSAGE );
                return;
            }

            // download the latest version of the Excel spreadsheet
            File excelFile = downloadExcelFile(dropboxLink);

            // parse the newly downloaded spreadsheet
            List<String> experimentIds = experimentParser_.parse(excelFile);

            // populate the dropdown with Experiment IDs
            updateExperimentList(experimentIds);

        }
        catch (Exception e) {
            JOptionPane.showMessageDialog( this, "Could not refresh experiment list:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE );
            e.printStackTrace();
        }
    }

    // update experiment ID dropdown
    private void updateExperimentList(List<String> experimentIds) {

        experimentComboBox_.removeAllItems();

        for (String experimentId : experimentIds) {
            experimentComboBox_.addItem(experimentId);
        }
    }

    // load experiments from the Excel file
    private void loadExperiments() {

        try {
            String dropboxLink = preferences_.get( DROPBOX_LINK_KEY, DEFAULT_DROPBOX_LINK );

            if (dropboxLink.equals("PASTE_YOUR_DROPBOX_LINK_HERE")) {
                JOptionPane.showMessageDialog( this, "No Dropbox link has been configured yet.", "Dropbox Link", JOptionPane.INFORMATION_MESSAGE );
                return;
            }

            File excelFile = downloadExcelFile(dropboxLink);
            List<String> experimentIds = experimentParser_.parse(excelFile);

            updateExperimentList(experimentIds);

        }
        catch (Exception e) {
            JOptionPane.showMessageDialog( this, "Could not load experiment list:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE );
            e.printStackTrace();
        }
    }

    // download the Excel file from Dropbox using the provided link
    private File downloadExcelFile(String dropboxLink) throws IOException {

        // dropbox shared links normally use "dl=0" for a browser/view link. Change it to "dl=1" so that Dropbox returns the actual file for downloading
        if (dropboxLink.contains("dl=0")) {
            dropboxLink = dropboxLink.replace("dl=0", "dl=1");
        }

        // create a URL connection to the Dropbox file
        URL url = new URL(dropboxLink);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(true); // allow the connection to follow redirects
        connection.setConnectTimeout(10000); // prevent plugin from waiting forever
        connection.setReadTimeout(30000);

        // create temp file for Apache POI to read the spreadsheet
        File tempFile = File.createTempFile( "Experimental_Plans_", ".xlsx" );

        // open downloaded data and write it to the temp file
        try (
            InputStream input = connection.getInputStream();
            FileOutputStream output = new FileOutputStream(tempFile)
        ) {
            byte[] buffer = new byte[8192];

            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }

        // close HTTP connection
        connection.disconnect();

        return tempFile;
    }

    // change the Dropbox link to the Excel file
    private void changeDropboxLink() {

        String currentLink = preferences_.get( DROPBOX_LINK_KEY, "" );

        String newLink = JOptionPane.showInputDialog( this, "Enter the Dropbox link to the Excel file:", currentLink );

        if (newLink == null) {
            return;
        }

        newLink = newLink.trim();

        if (newLink.isEmpty()) {
            return;
        }

        preferences_.put( DROPBOX_LINK_KEY, newLink );

        JOptionPane.showMessageDialog( this, "Dropbox link saved." );

        loadExperiments();
    }

    // apply the selected experiment ID to the root folder
    private void applyExperiment() {

        // get currect Experiment ID selected
        String experimentId = (String) experimentComboBox_.getSelectedItem();

        if (experimentId == null || experimentId.isEmpty()) {
            JOptionPane.showMessageDialog( this, "Please select an Experiment ID.", "No Experiment Selected", JOptionPane.WARNING_MESSAGE );
            return;
        }

        // generate today's data in the format YYYYMMDD
        String date = LocalDate.now().format( DateTimeFormatter.ofPattern("yyyyMMdd") );

        String folderName = date + "-" + experimentId;

        SequenceSettings currentSettings = studio_.acquisitions().getAcquisitionSettings();
        
        // Get whatever root is currently set in Micro-Manager
        String currentRoot = currentSettings.root();

        File currentRootFolder = new File(currentRoot);

        // if the current root is already one of our generated experiment folders, go up one level first
        String currentFolderName = currentRootFolder.getName();

        if (currentFolderName.matches( "\\d{8}-TCA-\\d{4}-\\d+")) {
            currentRootFolder = currentRootFolder.getParentFile();
        }

        // add the newly selected experiment folder
        File newRoot = new File(currentRootFolder, folderName);

        // convert the file back to absolute path for Micro-Manager to use
        String newRootPath = newRoot.getAbsolutePath();

        SequenceSettings newSettings = currentSettings.copyBuilder() .root(newRootPath) .build();

        studio_.acquisitions().setAcquisitionSettings( newSettings );

        // show path in plugin GUI
        rootFolderLabel_.setText(newRootPath);
    }

    private void openDropboxFile() {
        try {
            String dropboxLink =
                preferences_.get(DROPBOX_LINK_KEY, DEFAULT_DROPBOX_LINK);

            if (dropboxLink.isEmpty()
                    || dropboxLink.equals("PASTE_YOUR_DROPBOX_LINK_HERE")) {

                JOptionPane.showMessageDialog(
                    this,
                    "No Dropbox link has been configured yet.",
                    "Dropbox Link",
                    JOptionPane.INFORMATION_MESSAGE
                );

                return;
            }

            // Open the Dropbox link in the user's default browser
            Desktop.getDesktop().browse(new URI(dropboxLink));

        }
        catch (Exception e) {

            JOptionPane.showMessageDialog(
                this,
                "Could not open Dropbox link:\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
