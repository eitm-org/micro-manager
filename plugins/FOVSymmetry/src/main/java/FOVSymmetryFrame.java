/**
 * 
 */

package org.micromanager.plugins.fovsymmetry;

import com.google.common.eventbus.Subscribe;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import net.miginfocom.swing.MigLayout;
import org.micromanager.Studio;
import org.micromanager.data.Image;
import org.micromanager.events.ExposureChangedEvent;
import org.micromanager.internal.utils.WindowPositioning;
import org.micromanager.events.LiveModeEvent;
import ij.process.ImageProcessor;
import org.micromanager.acquisition.AcquisitionStartedEvent;
import org.micromanager.data.DataProvider;
import org.micromanager.data.DataProviderHasNewImageEvent;
import org.micromanager.data.Image;
import org.micromanager.display.DataViewer;
import javax.swing.JOptionPane;
import java.io.File;

// Imports for MMStudio internal packages
// Plugins should not access internal packages, to ensure modularity and
// maintainability. However, this plugin code is older than the current
// MMStudio API, so it still uses internal classes and interfaces. New code
// should not imitate this practice.


public class FOVSymmetryFrame extends JFrame {

   private Studio studio_;
   private JTextField userText_;
   private JTextField modelPathField_;
   private final JLabel decisionLabel_;
   private static Object FOVSymHandler_ = null;
   private DataProvider dataProvider_;
   private FOVQualityDecisionFunction.FOVModel fovModel_;
   private final JLabel iconLabel_;
   private ImageIcon iconOn_;
   private ImageIcon iconOff_;
   
   // autoStart on new active window
   private boolean enabled_ = true;
   // acquisition plot start on first image delivered.
   private boolean delayedStart_ = false;
   // A reference to the event handling (only?) instance
   private static Object RThandler_ = null;


   private static String modelPath_ = "C:\\Users\\AndreyAndreev\\Documents\\GitHub\\micro-manager-emi\\plugins\\FOVSymmetry\\src\\test\\resources\\test_fov_model.json";
   public FOVSymmetryFrame(Studio studio) {
      super("FOV Symmetry Plugin GUI");
      studio_ = studio;
      if (RThandler_ == null) {
         RThandler_ = this;
      }
      if (fovModel_ == null && modelPath_ != null) {
         try {
            fovModel_ = FOVQualityDecisionFunction.loadModelFromJson(modelPath_);
         } catch (Exception ex) {
            studio_.logs().logError("Failed to load FOV Symmetry model: " + ex.getMessage());
         }
      }

      super.setLayout(new MigLayout("fill, insets 2, gap 2, flowx"));

      JLabel title = new JLabel("FOV quality plugin");
      title.setFont(new Font("Arial", Font.BOLD, 14));
      super.add(title, "span, alignx center, wrap");

      // Model loading controls for FOV symmetry.
      super.add(new JLabel("Filepath for model JSON:"));
      modelPathField_ = new JTextField(15);
      if (modelPath_ != null) {
         modelPathField_.setText(modelPath_);
      }
      super.add(modelPathField_, "split");
      JButton loadModelButton = new JButton("Load Model");
      loadModelButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            String modelPath_ = modelPathField_.getText().trim();
            if (modelPath_.isEmpty()) {
               JOptionPane.showMessageDialog(FOVSymmetryFrame.this,
                       "Please provide a filepath for the model.", "Load model", JOptionPane.WARNING_MESSAGE);
               return;
            }
            try {
               fovModel_ = FOVQualityDecisionFunction.loadModelFromJson(modelPath_);
               decisionLabel_.setText("Model loaded successfully");
            } catch (Exception ex) {
               studio_.logs().logError("Failed to load FOV Symmetry model: " + ex.getMessage());
            }
         }
      });
      super.add(loadModelButton, "wrap");

      JCheckBox enabledCheckBox_ = new JCheckBox("Enable FOV evaluation");

      // Set initial state
      enabledCheckBox_.setSelected(enabled_);

      // Add listener to update variable
      enabledCheckBox_.addItemListener(new ItemListener() {
         @Override
         public void itemStateChanged(ItemEvent e) {
               enabled_ = (e.getStateChange() == ItemEvent.SELECTED);
         }
      });
      super.add(enabledCheckBox_, "wrap");


      try {
         iconOff_ = new ImageIcon(getClass().getResource("/org/micromanager/icons/off-bulb.png"));
      } catch (Exception ex) {
         studio_.logs().logError("Failed to load icon: " + ex.getMessage());
      }
      try {
         iconOn_ = new ImageIcon(getClass().getResource("/org/micromanager/icons/on-bulb.png"));
      } catch (Exception ex) {
         studio_.logs().logError("Failed to load icon: " + ex.getMessage());
      }

      iconLabel_ = new JLabel(iconOff_);
      super.add(iconLabel_, "wrap");

      decisionLabel_ = new JLabel("Decision: n/a");
      super.add(decisionLabel_, "wrap");

      super.setIconImage(Toolkit.getDefaultToolkit().getImage(
            this.getClass().getResource("/org/micromanager/icons/microscope.gif")));
      super.setLocation(100, 100);
      WindowPositioning.setUpLocationMemory(this, this.getClass(), null);

      super.pack();

      // Registering this class for events means that its event handlers
      // (that is, methods with the @Subscribe annotation) will be invoked when
      // an event occurs. You need to call the right registerForEvents() method
      // to get events; this one is for the application-wide event bus, but
      // there's also Datastore.registerForEvents() for events specific to one
      // Datastore, and DisplayWindow.registerForEvents() for events specific
      // to one image display window.
      studio_.events().registerForEvents(this);
   }

   @Subscribe
   public void onLiveMode(LiveModeEvent event) {
      DataViewer viewer = studio_.displays().getActiveDataViewer();
      if (viewer == null) {
         return;
      }
      dataProvider_ = viewer.getDataProvider();

      if (!delayedStart_) {
         decisionLabel_.setText("Waiting for images...");
         dataProvider_.registerForEvents(RThandler_); 
      } else {
         delayedStart_ = false;
      }
   }

   @Subscribe
   public void onNewAcquisition(AcquisitionStartedEvent event) {
      if (!enabled_) {
         return;
      }
      delayedStart_ = true;
      event.getDatastore().registerForEvents(RThandler_);
   }

   @Subscribe
   public void onNewImage(DataProviderHasNewImageEvent event) {
      processImage(event.getDataProvider(), event.getImage());
   }

   private void processImage(DataProvider dp, Image image) {
      if (fovModel_ != null) {
         try {
            if(enabled_){
               char decision = FOVQualityDecisionFunction.evaluate(fovModel_, image);
               decisionLabel_.setText("Decision: " + decision);
               if (decision == 'g') {
                  iconLabel_.setIcon(iconOn_);
               } else {
                  iconLabel_.setIcon(iconOff_);
               }
            }

         } catch (Exception ex) {
            decisionLabel_.setText("Decision: error");
            JOptionPane.showMessageDialog(FOVSymmetryFrame.this,
                     "Error during FOV decision evaluation: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
         }
      } else {
         decisionLabel_.setText("Decision: model not loaded");
      }
   }
   
}