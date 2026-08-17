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

public class ExperimentalPlanFrame extends JDialog{
    private final Studio studio_;

    private JButton applyButton_;
    private JButton refreshButton_;
    private JButton changeLinkButton_;

    private JComboBox<String> experimentComboBox_;

    public ExperimentalPlanFrame(Studio studio) {

        studio_ = studio;

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

        experimentComboBox_ = new JComboBox<>(
            new String[] {
                "TCA-2026-60",
                "TCA-2026-59",
                "TCA-2026-58"
            }
        );

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
}
