// fix until compiles with ant build-java

package org.micromanager.plugins.lightexposure;

import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import org.micromanager.MMPlugin;
import org.micromanager.Studio;
import org.micromanager.events.LiveModeEvent;
import org.micromanager.acquisition.AcquisitionStartedEvent;
import org.micromanager.data.DataProviderHasNewImageEvent;
import org.micromanager.data.Image;


public class UVExposure{

    private final Studio studio_;
    private final List<ExposureRow> exposureTable_= new ArrayList<>();

    @Override
    public void setContext(Studio studio) {
        studio_ = studio;
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

    private void addExposure(double x, double y, String filter, double timeMs){
        exposureTable_.add(new ExposureRow(x,y,filter,timeMs));
        System.out.println("Added exposure: ")+ exposureTable_.get(exposureTable_.size()-1);
    }

    @Subscribe
    public void onLiveMode(LiveModeEvent event){
        if (event.isOn()) {
            liveStartTimeMs = System.currentTimeMillis();
            liveOn = true;
        } else {
            liveOn = false;
            long duration = System.currentTimeMillis() - liveStartTimeMs;
            double x = studio_.core().get2DPositionX();
            double y = studio_.core().get2DPositionY();
            String filter = studio_.core().getString("FilterWheel", "Label");
            addExposure(x,y,filter,duration);
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
        //code
    }
    }
}


