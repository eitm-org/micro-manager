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

import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class ExperimentalPlanFrame extends JDialog{
    private final Studio studio_;

    private JButton applyButton_;
    private JButton refreshButton_;
    private JButton changeLinkButton_;

    private final ExperimentParser experimentParser_;
    private JComboBox<String> experimentComboBox_;

    public ExperimentalPlanFrame(Studio studio) {

        studio_ = studio;

        experimentParser_ = new ExperimentParser();

        setTitle("CAI Experiment ID");

        setSize(700, 200);

        setLocationRelativeTo(null);

        createUserInterface();
    }


    private void createUserInterface() {

        JPanel mainPanel = new JPanel(new BorderLayout());


        // title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.add(new JLabel("CAI Experiment ID"));

        // controls
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;

        controlsPanel.add(new JLabel("Planned experiments:"),gbc);

        experimentComboBox_ = new JComboBox<>();

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        controlsPanel.add(experimentComboBox_, gbc);

        applyButton_ = new JButton("Apply");

        gbc.gridx = 1;
        gbc.gridy = 1;

        controlsPanel.add(applyButton_, gbc);

        refreshButton_ = new JButton("<html>Refresh list of<br>Experiment IDs</html>");
        refreshButton_.addActionListener(e -> refreshExperiments());

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 1;

        controlsPanel.add(refreshButton_, gbc);

        changeLinkButton_ = new JButton("<html>Change link<br>to dropbox</html>");

        gbc.gridx = 3;
        gbc.gridy = 0;

        controlsPanel.add(changeLinkButton_, gbc);      

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(new JLabel("Root folder will become: "));

        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(controlsPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
   }

    private void refreshExperiments() {

        JOptionPane.showMessageDialog(
            this,
            "1. Refresh clicked"
        );

        JFileChooser fileChooser = new JFileChooser();

        int result = fileChooser.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File excelFile = fileChooser.getSelectedFile();

        JOptionPane.showMessageDialog(
            this,
            "2. File selected:\n" + excelFile.getName()
        );

        try {

            JOptionPane.showMessageDialog(
                this,
                "3. About to call parser"
            );

            List<String> experimentIds =
                experimentParser_.parse(excelFile);

            JOptionPane.showMessageDialog(
                this,
                "4. Parser finished!\n\n"
                + experimentIds
            );

            updateExperimentList(experimentIds);

            JOptionPane.showMessageDialog(
                this,
                "5. Dropdown updated!"
            );

        }
        catch (Throwable e) {

            JOptionPane.showMessageDialog(
                this,
                "ERROR:\n\n"
                + e.toString()
                + "\n\n"
                + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );

            e.printStackTrace();
        }
    }

    private void updateExperimentList(List<String> experimentIds) {

        experimentComboBox_.removeAllItems();

        for (String experimentId : experimentIds) {
            experimentComboBox_.addItem(experimentId);
        }
    }
}
