package net.onrc.onos.of.ctl;

import org.projectfloodlight.openflow.protocol.OFVersion;

import org.onlab.onos.of.controller.impl.registry.IControllerRegistry;

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
    public IOFSwitch getSwitchImpl(String mfr, String hwDesc, String swDesc, OFVersion ofv);

    /**
     * Returns the mastership registry used during controller-switch role election.
     * @return the registry
     */
    public IControllerRegistry getRegistry();

}
