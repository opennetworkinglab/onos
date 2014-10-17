package org.onlab.onos.optical.cfg;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Public class corresponding to JSON described data model.
 */
public class OpticalNetworkConfig {
    protected static final Logger log = LoggerFactory.getLogger(OpticalNetworkConfig.class);

    private List<OpticalSwitchDescription> opticalSwitches;
    private List<OpticalLinkDescription> opticalLinks;

    public OpticalNetworkConfig() {
        opticalSwitches = new ArrayList<OpticalSwitchDescription>();
        opticalLinks = new ArrayList<OpticalLinkDescription>();
    }

    public List<OpticalSwitchDescription> getOpticalSwitches() {
        return opticalSwitches;
    }

    public void setOpticalSwitches(List<OpticalSwitchDescription> switches) {
        this.opticalSwitches = switches;
    }

    public List<OpticalLinkDescription> getOpticalLinks() {
        return opticalLinks;
    }

    public void setOpticalLinks(List<OpticalLinkDescription> links) {
        this.opticalLinks = links;
    }

}

