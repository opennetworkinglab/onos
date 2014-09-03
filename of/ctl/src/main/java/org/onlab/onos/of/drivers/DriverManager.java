package org.onlab.onos.of.drivers;



import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.driver.OpenFlowSwitchDriver;
import org.onlab.onos.of.controller.driver.OpenFlowSwitchDriverFactory;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of a driver manager that differentiates between
 * connected switches using the OF Description Statistics Reply message.
 */
public final class DriverManager implements OpenFlowSwitchDriverFactory {

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
    @Override
    public OpenFlowSwitchDriver getOFSwitchImpl(Dpid dpid,
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
        return null;
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

    public static OpenFlowSwitchDriver getSwitch(Dpid dpid,
            OFDescStatsReply desc, OFVersion ofv) {
        return new DriverManager().getOFSwitchImpl(dpid, desc, ofv);
    }

}
