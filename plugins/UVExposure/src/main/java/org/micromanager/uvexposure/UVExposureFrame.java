// fix until compiles with ant build-java

package org.micromanager.plugins.uvexposure;

import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JFrame;
import org.micromanager.events.LiveModeEvent;
import org.micromanager.acquisition.AcquisitionStartedEvent;
import org.micromanager.data.DataProviderHasNewImageEvent;
import org.micromanager.data.Image;
import javax.swing.JOptionPane;
import org.micromanager.MenuPlugin;
import org.micromanager.Studio;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import javax.swing.SwingUtilities;

public class UVExposureFrame extends JFrame {

    private Studio studio_;
    private final List<ExposureRow> exposureTable_= new ArrayList<>();
    private JTable table_;
    private DefaultTableModel tableModel_;


    public UVExposureFrame(Studio studio) {
        super("UV Exposure Table");
        studio_ = studio;
        studio_.events().registerForEvents(this);

        tableModel_ = new DefaultTableModel(
            new String[]{"X", "Y", "Filter", "Exposure (ms)"},
            0
        );

        table_ = new JTable(tableModel_);
        JScrollPane scrollPane = new JScrollPane(table_);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }



    private static class ExposureRow {
        double x;
        double y;
        String filter;
        double timeMs;

        ExposureRow(double x, double y, String filter, double timeMs){
            this.x = x;
            this.y = y;
            this.filter = filter;
            this.timeMs = timeMs;
        }
    }

    private void addExposure(double x, double y, String filter, double timeMs) {
        exposureTable_.add(new ExposureRow(x, y, filter, timeMs));

        SwingUtilities.invokeLater(() -> {
            tableModel_.addRow(new Object[]{
                x, y, filter, timeMs
            });
        });

        System.out.println("Added exposure row");
    }

     /* 
    @Subscribe
    public void onLiveMode(LiveModeEvent event) {
        System.out.println("LiveModeEvent fired: " + event.isOn());

        SwingUtilities.invokeLater(() -> {
            tableModel_.addRow(new Object[]{
                -1, -1, "LIVE_MODE", event.isOn() ? 1.0 : 0.0
            });
        });
    }



    @Subscribe
    public void onNewAcquisition(AcquisitionStartedEvent event){
        if (!autoStart_ || manager_ == null) {
            return;
        }
        if (!event.isOn()) {
            return;
        }
        // get time
    }
    
   
    @Subscribe
    public void onNewImage(DataProviderHasNewImageEvent event){
        Image img = event.getImage();
        if (img==null) return;
        // double x = studio_.core().get2DPositionX();
        // double y = studio_.core().get2DPositionY();
        // String filter = studio_.core().getString("FilterWheel", "Label");
        double x = 2;
        double y = 1;
        String filter = "NADH";
        double duration = img.getMetadata().getExposureMs();

        addExposure(x,y,filter,duration);

    }

*/

    @Subscribe
    public void onNewAcquisition(AcquisitionStartedEvent event) {
        event.getDatastore().registerForEvents(this); // without this, onNewImage won't fire
    }


    @Subscribe
    public void onNewImage(DataProviderHasNewImageEvent event) {
        if (event.getDataProvider() == null) {
            return;
        }

        Image img = event.getImage();
        if (img == null) {
            return;
        }

        // Example coordinates and filter (replace with real values if you have them)
        double x = img.getMetadata().getXPositionUm();
        double y = img.getMetadata().getYPositionUm();
        String filter = img.getMetadata().getUUID();
        double duration = img.getMetadata().getExposureMs();

        // Add the exposure to your table
        addExposure(x, y, filter, duration);

    }
}




