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
import org.micromanager.display.DataViewer;
import org.micromanager.data.DataProvider;
import com.google.common.eventbus.Subscribe;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.miginfocom.swing.MigLayout;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.micromanager.Studio;
import org.micromanager.acquisition.AcquisitionStartedEvent;
import org.micromanager.data.DataProvider;
import org.micromanager.data.DataProviderHasNewImageEvent;
import org.micromanager.data.Image;
import org.micromanager.display.DataViewer;
import org.micromanager.events.LiveModeEvent;
import org.micromanager.internal.utils.WindowPositioning;
import org.micromanager.propertymap.MutablePropertyMapView;
import java.util.Collections;

public class UVExposureFrame extends JFrame {

    private Studio studio_;
    private final List<ExposureRow> exposureTable_= new ArrayList<>();
    private JTable table_;
    private DefaultTableModel tableModel_;

    public UVExposureFrame(Studio studio) {
        super("UV Exposure Table");
        studio_ = studio;
        studio_.events().registerForEvents(this);

        tableModel_ = new DefaultTableModel(new String[]{"X", "Y", "Filter", "Exposure (ms)"}, 0);
        
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

    private ExposureRow findMatchingRow(double x, double y, String filter) {
        for (ExposureRow row : exposureTable_) {
            if (row.filter.equals(filter) && Math.abs(row.x - x) < 1e-6 && Math.abs(row.y - y) < 1e-6) {
                return row;
            }
        }
        return null;
    }


    private void addExposure(double x, double y, String filter, double timeMs) {
        ExposureRow existing = findMatchingRow(x, y, filter);

        // if row with same position and filter exists
        if (existing != null) {
            existing.timeMs += timeMs;

            int rowIndex = exposureTable_.indexOf(existing);

            SwingUtilities.invokeLater(() -> {
                tableModel_.setValueAt(existing.timeMs, rowIndex, 3);
            });

        // if row with same position and filter does not exist
        } else {
            ExposureRow newRow = new ExposureRow(x, y, filter, timeMs);
            exposureTable_.add(newRow);

            SwingUtilities.invokeLater(() -> {
                tableModel_.addRow(new Object[]{
                    x, y, filter, timeMs
                });
            });
        }
    }


    @Subscribe
    public void onLiveMode(LiveModeEvent event) {
        if (!event.isOn()) return;

        DataViewer viewer = studio_.displays().getActiveDataViewer();
        if (viewer == null) return;

        viewer.getDataProvider().registerForEvents(this);
    }



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

        double x = img.getMetadata().getXPositionUm();
        double y = img.getMetadata().getYPositionUm();        

        int channelIndex = img.getCoords().getC();
        DataProvider dp = event.getDataProvider();
        List<String> channelNames = dp.getSummaryMetadata().getChannelNameList();
        String filter = channelNames.get(channelIndex);


       // String filter = "NADH";
        double duration = img.getMetadata().getExposureMs();

        addExposure(x, y, filter, duration);
    }

}




