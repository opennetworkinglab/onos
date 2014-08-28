package org.onlab.onos.of.controller.impl.internal;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.OpenFlowController;
import org.onlab.onos.of.controller.OpenFlowSwitch;
import org.onlab.onos.of.controller.OpenFlowSwitchListener;
import org.onlab.onos.of.controller.PacketListener;
import org.onlab.onos.of.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service
public class OpenFlowControllerImpl implements OpenFlowController {

    protected ConcurrentHashMap<Long, OpenFlowSwitch> connectedSwitches;
    protected ConcurrentHashMap<Long, OpenFlowSwitch> activeMasterSwitches;
    protected ConcurrentHashMap<Long, OpenFlowSwitch> activeEqualSwitches;

    protected OpenFlowSwitchAgent agent = new OpenFlowSwitchAgent();
    protected ArrayList<OpenFlowSwitchListener> ofEventListener;

    private final Controller ctrl = new Controller();

    @Activate
    public void activate() {
        ctrl.start(agent);
    }

    @Deactivate
    public void deactivate() {
        ctrl.stop();
    }

    @Override
    public Iterable<OpenFlowSwitch> getSwitches() {
        return connectedSwitches.values();
    }

    @Override
    public Iterable<OpenFlowSwitch> getMasterSwitches() {
        return activeMasterSwitches.values();
    }

    @Override
    public Iterable<OpenFlowSwitch> getEqualSwitches() {
        return activeEqualSwitches.values();
    }

    @Override
    public OpenFlowSwitch getSwitch(Dpid dpid) {
        return connectedSwitches.get(dpid.value());
    }

    @Override
    public OpenFlowSwitch getMasterSwitch(Dpid dpid) {
        return activeMasterSwitches.get(dpid.value());
    }

    @Override
    public OpenFlowSwitch getEqualSwitch(Dpid dpid) {
        return activeEqualSwitches.get(dpid.value());    }

    @Override
    public void addListener(OpenFlowSwitchListener listener) {
        if (!ofEventListener.contains(listener)) {
            this.ofEventListener.add(listener);
        }
    }

    @Override
    public void removeListener(OpenFlowSwitchListener listener) {
        this.ofEventListener.remove(listener);
    }

    @Override
    public void addPacketListener(int priority, PacketListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removePacketListener(PacketListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void write(Dpid dpid, OFMessage msg) {
        this.getSwitch(dpid).write(msg);
    }

    @Override
    public void processPacket(OFMessage msg) {
    }

    @Override
    public void setRole(Dpid dpid, RoleState role) {
        switch (role) {
        case MASTER:
            agent.transitionToMasterSwitch(dpid.value());
            break;
        case EQUAL:
            agent.transitionToEqualSwitch(dpid.value());
            break;
        case SLAVE:
            //agent.transitionToSlaveSwitch(dpid.value());
            break;
        default:
            //WTF role is this?
        }

    }

    public class OpenFlowSwitchAgent {

        private final Logger log = LoggerFactory.getLogger(OpenFlowSwitchAgent.class);
        private Lock switchLock = new ReentrantLock();

        public boolean addConnectedSwitch(long dpid, AbstractOpenFlowSwitch sw) {
            if (connectedSwitches.get(dpid) != null) {
                log.error("Trying to add connectedSwitch but found a previous "
                        + "value for dpid: {}", dpid);
                return false;
            } else {
                log.error("Added switch {}", dpid);
                connectedSwitches.put(dpid, sw);
                for (OpenFlowSwitchListener l : ofEventListener) {
                    l.switchAdded(new Dpid(dpid));
                }
                return true;
            }
        }

        private boolean validActivation(long dpid) {
            if (connectedSwitches.get(dpid) == null) {
                log.error("Trying to activate switch but is not in "
                        + "connected switches: dpid {}. Aborting ..",
                        HexString.toHexString(dpid));
                return false;
            }
            if (activeMasterSwitches.get(dpid) != null ||
                    activeEqualSwitches.get(dpid) != null) {
                log.error("Trying to activate switch but it is already "
                        + "activated: dpid {}. Found in activeMaster: {} "
                        + "Found in activeEqual: {}. Aborting ..", new Object[] {
                                HexString.toHexString(dpid),
                                (activeMasterSwitches.get(dpid) == null) ? 'N' : 'Y',
                                (activeEqualSwitches.get(dpid) == null) ? 'N' : 'Y'});
                return false;
            }
            return true;
        }

        /**
         * Called when a switch is activated, with this controller's role as MASTER.
         */
        protected boolean addActivatedMasterSwitch(long dpid, AbstractOpenFlowSwitch sw) {
            switchLock.lock();
            try {
                if (!validActivation(dpid)) {
                    return false;
                }
                activeMasterSwitches.put(dpid, sw);
                return true;
            } finally {
                switchLock.unlock();
            }
    }

        /**
         * Called when a switch is activated, with this controller's role as EQUAL.
         */
        protected boolean addActivatedEqualSwitch(long dpid, AbstractOpenFlowSwitch sw) {
            switchLock.lock();
            try {
                    if (!validActivation(dpid)) {
                        return false;
                    }
                    activeEqualSwitches.put(dpid, sw);
                    return true;
            } finally {
                switchLock.unlock();
            }
        }

        /**
         * Called when this controller's role for a switch transitions from equal
         * to master. For 1.0 switches, we internally refer to the role 'slave' as
         * 'equal' - so this transition is equivalent to 'addActivatedMasterSwitch'.
         */
        protected void transitionToMasterSwitch(long dpid) {
            switchLock.lock();
            try {
                OpenFlowSwitch sw = activeEqualSwitches.remove(dpid);
                if (sw == null) {
                    log.error("Transition to master called on sw {}, but switch "
                            + "was not found in controller-cache", dpid);
                    return;
                }
                activeMasterSwitches.put(dpid, sw);
            } finally {
                switchLock.unlock();
            }
        }


        /**
         * Called when this controller's role for a switch transitions to equal.
         * For 1.0 switches, we internally refer to the role 'slave' as
         * 'equal'.
         */
        protected void transitionToEqualSwitch(long dpid) {
            switchLock.lock();
            try {
                OpenFlowSwitch sw = activeMasterSwitches.remove(dpid);
                if (sw == null) {
                    log.error("Transition to equal called on sw {}, but switch "
                            + "was not found in controller-cache", dpid);
                    return;
                }
                activeEqualSwitches.put(dpid, sw);
            } finally {
                switchLock.unlock();
            }

        }

        /**
         * Clear all state in controller switch maps for a switch that has
         * disconnected from the local controller. Also release control for
         * that switch from the global repository. Notify switch listeners.
         */
        public void removeConnectedSwitch(long dpid) {
            connectedSwitches.remove(dpid);
            OpenFlowSwitch sw = activeMasterSwitches.remove(dpid);
            if (sw == null) {
                sw = activeEqualSwitches.remove(dpid);
            }
            for (OpenFlowSwitchListener l : ofEventListener) {
                l.switchRemoved(new Dpid(dpid));
            }
        }

        public void processMessage(OFMessage m) {
            processPacket(m);
        }
    }



}
