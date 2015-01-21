/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.igp.controller.impl;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.igp.controller.IGPController;
import org.onosproject.igp.controller.IgpDpid;
import org.onosproject.igp.controller.IgpSwitch;
import org.onosproject.igp.controller.IgpSwitchListener;
import org.onosproject.igp.controller.driver.IgpAgent;
import org.onosproject.net.flowext.FlowRuleBatchExtRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service
public class IgpControllerImpl implements IGPController {

    private static final Logger log =
            LoggerFactory.getLogger(IgpControllerImpl.class);

    protected ConcurrentHashMap<IgpDpid, IgpSwitch> connectedSwitches =
            new ConcurrentHashMap<IgpDpid, IgpSwitch>();

    protected IgpSwitchAgent agent = new IgpSwitchAgent();
    protected Set<IgpSwitchListener> igpSwitchListener = new HashSet<>();
    private final Controller ctrl = new Controller();

    @Activate
    public void activate() {
    	log.info("begin to listen");
        ctrl.start(agent);
    }

    @Deactivate
    public void deactivate() {
        ctrl.stop();
    }

    @Override
    public Iterable<IgpSwitch> getSwitches() {
        return connectedSwitches.values();
    }

    @Override
    public IgpSwitch getSwitch(IgpDpid dpid) {
        return connectedSwitches.get(dpid);
    }

    @Override
    public void addListener(IgpSwitchListener listener) {
        if (!igpSwitchListener.contains(listener)) {
            this.igpSwitchListener.add(listener);
        }
    }

    @Override
    public void removeListener(IgpSwitchListener listener) {
        this.igpSwitchListener.remove(listener);
    }

    @Override
    public void write(IgpDpid dpid, FlowRuleBatchExtRequest msg) {
        this.getSwitch(dpid).sendMsg(msg);
    }

    /**
     * Implementation of an OpenFlow Agent which is responsible for
     * keeping track of connected switches and the state in which
     * they are.
     */
    public class IgpSwitchAgent implements IgpAgent {

        private final Logger log = LoggerFactory.getLogger(IgpSwitchAgent.class);

        @Override
        public boolean addConnectedSwitch(IgpDpid dpid, IgpSwitch sw) {

            if (connectedSwitches.get(dpid) != null) {
                log.error("Trying to add connectedSwitch but found a previous "
                        + "value for dpid: {}", dpid);
                return false;
            } else {
                log.info("Added switch {}", dpid);
                connectedSwitches.put(dpid, sw);
                for (IgpSwitchListener l : igpSwitchListener) {
                    l.switchAdded(dpid);
                }
                return true;
            }
        }

        @Override
        public void removeConnectedSwitch(IgpDpid dpid) {
            connectedSwitches.remove(dpid);
            for (IgpSwitchListener l : igpSwitchListener) {
                log.warn("removal for {}", dpid);
                l.switchRemoved(dpid);
            }
        }
    }
 
}
