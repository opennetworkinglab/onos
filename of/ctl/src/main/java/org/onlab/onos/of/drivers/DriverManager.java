package org.onlab.onos.of.drivers;


import java.util.List;

import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.RoleState;
import org.onlab.onos.of.controller.impl.internal.AbstractOpenFlowSwitch;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of a driver manager that differentiates between
 * connected switches using the OF Description Statistics Reply message.
 */
public final class DriverManager {

    private static final Logger log = LoggerFactory.getLogger(DriverManager.class);

    // Whether to use an OF 1.3 configured TTP, or to use an OF 1.0-style
    // single table with packet-ins.
    private static boolean cpqdUsePipeline13 = false;

    /**
     * Return an IOFSwitch object based on switch's manufacturer description
     * from OFDescStatsReply.
     *
     * @param desc DescriptionStatistics reply from the switch
     * @return A IOFSwitch instance if the driver found an implementation for
     *         the given description. Otherwise it returns OFSwitchImplBase
     */
    public static AbstractOpenFlowSwitch getOFSwitchImpl(Dpid dpid,
            OFDescStatsReply desc, OFVersion ofv) {
        String vendor = desc.getMfrDesc();
        String hw = desc.getHwDesc();
        if (vendor.startsWith("Stanford University, Ericsson Research and CPqD Research")
                &&
                hw.startsWith("OpenFlow 1.3 Reference Userspace Switch")) {
            return new OFSwitchImplCPqD13(dpid, desc, cpqdUsePipeline13);
        }

        if (vendor.startsWith("Nicira") &&
                hw.startsWith("Open vSwitch")) {
            if (ofv == OFVersion.OF_10) {
                return new OFSwitchImplOVS10(dpid, desc);
            } else if (ofv == OFVersion.OF_13) {
                return new OFSwitchImplOVS13(dpid, desc);
            }
        }

        log.warn("DriverManager could not identify switch desc: {}. "
                + "Assigning OFSwitchImplBase", desc);
        AbstractOpenFlowSwitch base = new AbstractOpenFlowSwitch(dpid) {

            @Override
            public void write(List<OFMessage> msgs) {
                // TODO Auto-generated method stub
            }

            @Override
            public void sendMsg(OFMessage m) {
                // TODO Auto-generated method stub
            }

            @Override
            public Boolean supportNxRole() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void startDriverHandshake() {
                // TODO Auto-generated method stub
            }

            @Override
            public void setFeaturesReply(OFFeaturesReply featuresReply) {
                // TODO Auto-generated method stub

            }

            @Override
            public void processDriverHandshakeMessage(OFMessage m) {
                // TODO Auto-generated method stub
            }

            @Override
            public boolean isDriverHandshakeComplete() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public RoleState getRole() {
                // TODO Auto-generated method stub
                return null;
            }
        };
        base.setSwitchDescription(desc);
        // XXX S must set counter here - unidentified switch
        return base;
    }

    /**
     * Private constructor to avoid instantiation.
     */
    private DriverManager() {
    }

    /**
     * Sets the configuration parameter which determines how the CPqD switch
     * is set up. If usePipeline13 is true, a 1.3 pipeline will be set up on
     * the switch. Otherwise, the switch will be set up in a 1.0 style with
     * a single table where missed packets are sent to the controller.
     *
     * @param usePipeline13 whether to use a 1.3 pipeline or not
     */
    public static void setConfigForCpqd(boolean usePipeline13) {
        cpqdUsePipeline13 = usePipeline13;
    }
}
