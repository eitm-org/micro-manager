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

// Imports for MMStudio internal packages
// Plugins should not access internal packages, to ensure modularity and
// maintainability. However, this plugin code is older than the current
// MMStudio API, so it still uses internal classes and interfaces. New code
// should not imitate this practice.


public class FOVSymmetryFrame extends JFrame {

   private Studio studio_;
   private JTextField userText_;
   private final JLabel imageInfoLabel_;
   private final JLabel exposureTimeLabel_;
   private static Object FOVSymHandler_ = null;
   private DataProvider dataProvider_;

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

      // Snap an image, show the image in the Snap/Live view, and show some
      // stats on the image in our frame.
      imageInfoLabel_ = new JLabel();
      super.add(imageInfoLabel_, "growx, split, span");
      JButton snapButton = new JButton("Snap Image");
      snapButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            // Multiple images are returned only if there are multiple
            // cameras. We only care about the first image.
            List<Image> images = studio_.live().snap(true);
            Image firstImage = images.get(0);
            showImageInfo(firstImage);
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
