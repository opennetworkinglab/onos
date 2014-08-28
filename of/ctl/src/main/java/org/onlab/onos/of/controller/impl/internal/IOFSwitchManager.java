package org.onlab.onos.of.controller.impl.internal;

import org.projectfloodlight.openflow.protocol.OFVersion;


/**
 * Interface to passed to controller class in order to allow
 * it to spawn the appropriate type of switch and furthermore
 * specify a registry object (ie. ZooKeeper).
 *
 */
public interface IOFSwitchManager {

    /**
     * Given a description string for a switch spawn the
     * concrete representation of that switch.
     *
     * @param mfr manufacturer description
     * @param hwDesc hardware description
     * @param swDesc software description
     * @param ofv openflow version
     * @return A switch of type IOFSwitch.
     */
    public AbstractOpenFlowSwitch getSwitchImpl(String mfr, String hwDesc, String swDesc, OFVersion ofv);

}
