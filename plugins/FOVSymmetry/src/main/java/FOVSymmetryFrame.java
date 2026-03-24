/**
 * 
 */

package org.micromanager.plugins.fovsymmetry;

import com.google.common.eventbus.Subscribe;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JButton;
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
   private final JLabel imageInfoLabel_;
   private final JLabel decisionLabel_;
   private final JLabel exposureTimeLabel_;
   private static Object FOVSymHandler_ = null;
   private DataProvider dataProvider_;
   private FOVQualityDecisionFunction.FOVModel fovModel_;

   public FOVSymmetryFrame(Studio studio) {
      super("FOV Symmetry Plugin GUI");
      studio_ = studio;

      super.setLayout(new MigLayout("fill, insets 2, gap 2, flowx"));

      JLabel title = new JLabel("I'm an example plugin!");
      title.setFont(new Font("Arial", Font.BOLD, 14));
      super.add(title, "span, alignx center, wrap");

      // Create a text field for the user to customize their alerts.
      super.add(new JLabel("Alert text: "));
      userText_ = new JTextField(30);
      userText_.setText("Something happened!");
      super.add(userText_);

      JButton alertButton = new JButton("Alert me!");
      // Clicking on this button will invoke the ActionListener, which in turn
      // will show a text alert to the user.
      alertButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            // Use the contents of userText_ as the text.
            studio_.alerts().postAlert("Example Alert!",
                  FOVSymmetryFrame.class, userText_.getText());
         }
      });
      super.add(alertButton, "wrap");

      // Model loading controls for FOV symmetry.
      super.add(new JLabel("Model .mat path (or other model source):"));
      modelPathField_ = new JTextField(30);
      super.add(modelPathField_, "split");
      JButton loadModelButton = new JButton("Load Model (stub)");
      loadModelButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            String modelPath = modelPathField_.getText().trim();
            if (modelPath.isEmpty()) {
               JOptionPane.showMessageDialog(FOVSymmetryFrame.this,
                       "Please provide a model path.", "Load model", JOptionPane.WARNING_MESSAGE);
               return;
            }
            // This code currently indicates loading is not implemented.
            JOptionPane.showMessageDialog(FOVSymmetryFrame.this,
                    "Model loading from .mat is not implemented yet.\n" +
                            "Please construct FOVQualityDecisionFunction.FOVModel directly", "Load model", JOptionPane.INFORMATION_MESSAGE);
         }
      });
      super.add(loadModelButton, "wrap");

      // Snap an image, show the image in the Snap/Live view, and show some
      // stats on the image in our frame.
      imageInfoLabel_ = new JLabel();
      super.add(imageInfoLabel_, "growx, split, span");
      decisionLabel_ = new JLabel("Decision: n/a");
      super.add(decisionLabel_, "wrap");
      JButton snapButton = new JButton("Snap Image");
      snapButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            // Multiple images are returned only if there are multiple
            // cameras. We only care about the first image.
            List<Image> images = studio_.live().snap(true);
            if (images.isEmpty()) {
               JOptionPane.showMessageDialog(FOVSymmetryFrame.this,
                       "No image was captured", "Snap image", JOptionPane.WARNING_MESSAGE);
               return;
            }
            Image firstImage = images.get(0);
            showImageInfo(firstImage);
            if (fovModel_ != null) {
               try {
                  char decision = FOVQualityDecisionFunction.evaluate(fovModel_, firstImage);
                  decisionLabel_.setText("Decision: " + decision);
               } catch (Exception ex) {
                  decisionLabel_.setText("Decision: error");
                  JOptionPane.showMessageDialog(FOVSymmetryFrame.this,
                          "Error during FOV decision evaluation: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
               }
            } else {
               decisionLabel_.setText("Decision: model not loaded");
            }
         }
      });
      super.add(snapButton, "wrap");

      exposureTimeLabel_ = new JLabel("");
      super.add(exposureTimeLabel_, "split, span, growx");

           super.setIconImage(Toolkit.getDefaultToolkit().getImage(
            getClass().getResource("/org/micromanager/icons/microscope.gif")));
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

      updateSymmetryValue();
   }
   private void updateSymmetryValue() {
      // Do something to update the symmetry value.
   }
   /**
    * Display some information on the data in the provided image.
    */
   private void showImageInfo(Image image) {
      // See DisplayManager for information on these parameters.
      //HistogramData data = studio_.displays().calculateHistogram(
      //   image, 0, 16, 16, 0, true);
      imageInfoLabel_.setText(String.format(
            "Image size: %dx%d", // min: %d, max: %d, mean: %d, std: %.2f",
            image.getWidth(), image.getHeight())); //, data.getMinVal(),
      //data.getMaxVal(), data.getMean(), data.getStdDev()));
   }
}
