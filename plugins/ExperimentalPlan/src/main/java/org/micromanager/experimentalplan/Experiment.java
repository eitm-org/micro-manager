/**
 * A Micro-Manager plugin, intended to organize experimental plans.
 *  *
 * <p>Copy this code to a location of your choice, change the name of the project
 * (and the classes), build the jar file and copy it to the mmplugins folder
 * in your Micro-Manager directory.
 *
 * <p>Once you have it loaded and running, you can attach the NetBean debugger
 * and use all of NetBean's functionality to debug your code.  If you make a
 * generally useful plugin, please do not hesitate to send a copy to
 * info@micro-manager.org for inclusion in the Micro-Manager source code
 * repository.
 *
 * <p>LICENSE:      This file is distributed under the BSD license.
 * License text is included with the source distribution.
 * This file is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
 *
 * @author Fiona Ryan
 */

package org.micromanager.experimentalplan;

public class Experiment {
    private final String id;
    private final String purpose;

    public Experiment(String id, String purpose) {
        this.id = id;
        this.purpose = purpose;
    }

    public String getId() {
        return id;
    }

    public String getPurpose() {
        return purpose;
    }

    @Override
    public String toString() {
        return id + " - " + purpose;
    }
}