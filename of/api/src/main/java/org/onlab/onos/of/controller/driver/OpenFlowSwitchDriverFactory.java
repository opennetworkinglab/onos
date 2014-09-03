package org.onlab.onos.of.controller.driver;

import org.onlab.onos.of.controller.Dpid;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFVersion;

/**
 * Switch factory which returns concrete switch objects for the
 * physical openflow switch in use.
 *
 */
public interface OpenFlowSwitchDriverFactory {


    /**
     * Constructs the real openflow switch representation.
     * @param dpid the dpid for this switch.
     * @param desc its description.
     * @param ofv the OF version in use
     * @return the openflow switch representation.
     */
    public OpenFlowSwitchDriver getOFSwitchImpl(Dpid dpid,
            OFDescStatsReply desc, OFVersion ofv);
}
