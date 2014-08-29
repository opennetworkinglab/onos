package org.onlab.onos.of.drivers;

import java.util.concurrent.atomic.AtomicBoolean;

import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.impl.internal.AbstractOpenFlowSwitch;
import org.onlab.onos.of.controller.impl.internal.SwitchDriverSubHandshakeAlreadyStarted;
import org.onlab.onos.of.controller.impl.internal.SwitchDriverSubHandshakeCompleted;
import org.onlab.onos.of.controller.impl.internal.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Nicira, Inc. Make
 * (Hardware Desc.) : Open vSwitch Model (Datapath Desc.) : None Software :
 * 2.1.0 (or whatever version + build) Serial : None
 */
public class OFSwitchImplOVS13 extends AbstractOpenFlowSwitch {

    private static Logger log =
            LoggerFactory.getLogger(OFSwitchImplOVS13.class);

    private AtomicBoolean driverHandshakeComplete;
    private OFFactory factory;
    private long barrierXidToWaitFor = -1;

    public OFSwitchImplOVS13(Dpid dpid, OFDescStatsReply desc) {
        super(dpid);
        driverHandshakeComplete = new AtomicBoolean(false);
        setSwitchDescription(desc);
    }

    @Override
    public String toString() {
        return "OFSwitchImplOVS13 [" + ((channel != null)
                ? channel.getRemoteAddress() : "?")
                + " DPID[" + ((getStringId() != null) ? getStringId() : "?") + "]]";
    }

    @Override
    public void startDriverHandshake() {
        log.debug("Starting driver handshake for sw {}", getStringId());
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;
        factory = factory();
        configureSwitch();
    }

    @Override
    public boolean isDriverHandshakeComplete() {
        if (!startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeNotStarted();
        }
        return driverHandshakeComplete.get();
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {
        if (!startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeNotStarted();
        }
        if (driverHandshakeComplete.get()) {
            throw new SwitchDriverSubHandshakeCompleted(m);
        }

        switch (m.getType()) {
        case BARRIER_REPLY:
            if (m.getXid() == barrierXidToWaitFor) {
                driverHandshakeComplete.set(true);
            }
            break;

        case ERROR:
            log.error("Switch {} Error {}", getStringId(), (OFErrorMsg) m);
            break;

        case FEATURES_REPLY:
            break;
        case FLOW_REMOVED:
            break;
        case GET_ASYNC_REPLY:
            // OFAsyncGetReply asrep = (OFAsyncGetReply)m;
            // decodeAsyncGetReply(asrep);
            break;

        case PACKET_IN:
            break;
        case PORT_STATUS:
            break;
        case QUEUE_GET_CONFIG_REPLY:
            break;
        case ROLE_REPLY:
            break;

        case STATS_REPLY:
            // processStatsReply((OFStatsReply) m);
            break;

        default:
            log.debug("Received message {} during switch-driver subhandshake "
                    + "from switch {} ... Ignoring message", m, getStringId());

        }
    }


    private void configureSwitch() {
        sendBarrier(true);
    }


    private void sendBarrier(boolean finalBarrier) {
        int xid = getNextTransactionId();
        if (finalBarrier) {
            barrierXidToWaitFor = xid;
        }
        OFBarrierRequest br = factory
                .buildBarrierRequest()
                .setXid(xid)
                .build();
        sendMsg(br);
    }

    @Override
    public void sendMsg(OFMessage m) {
        channel.write(m);
    }

    @Override
    public Boolean supportNxRole() {
        return false;
    }
}
