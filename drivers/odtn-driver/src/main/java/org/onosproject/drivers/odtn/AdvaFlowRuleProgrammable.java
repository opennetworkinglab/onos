/*
 * Copyright 2018-present Open Networking Foundation
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

 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn;

import com.google.common.collect.ImmutableList;
import org.onlab.util.Frequency;
import org.onosproject.drivers.odtn.impl.DeviceConnectionCache;
import org.onosproject.drivers.odtn.impl.FlowRuleParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_NAME;

/**
 * Implementation of FlowRuleProgrammable interface for
 * OpenConfig terminal devices.
 */
public class AdvaFlowRuleProgrammable
        extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    private static final Logger log =
            LoggerFactory.getLogger(AdvaFlowRuleProgrammable.class);

    /**
     * Apply the flow entries specified in the collection rules.
     *
     * @param rules A collection of Flow Rules to be applied
     * @return The collection of added Flow Entries
     */
    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            openConfigError("null session");
            return ImmutableList.of();
        }
        List<FlowRule> added = new ArrayList<>();
        for (FlowRule r : rules) {
            try {
                String connectionId = applyFlowRule(session, r);
                getConnectionCache().add(did(), connectionId, r);
                added.add(r);
            } catch (Exception e) {
                openConfigError("Error {}", e);
                continue;
            }
        }
        openConfigLog("applyFlowRules added {}", added.size());
        return added;
    }

    /**
     * Get the flow entries that are present on the device.
     *
     * @return A collection of Flow Entries
     */
    @Override
    public Collection<FlowEntry> getFlowEntries() {
        DeviceConnectionCache cache = getConnectionCache();
        if (cache.get(did()) == null) {
            return ImmutableList.of();
        }

        List<FlowEntry> entries = new ArrayList<>();
        for (FlowRule r : cache.get(did())) {
            entries.add(
                    new DefaultFlowEntry(r, FlowEntry.FlowEntryState.ADDED, 0, 0, 0));
        }
        return entries;
    }

    /**
     * Remove the specified flow rules.
     *
     * @param rules A collection of Flow Rules to be removed
     * @return The collection of removed Flow Entries
     */
    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            openConfigError("null session");
            return ImmutableList.of();
        }
        List<FlowRule> removed = new ArrayList<>();
        for (FlowRule r : rules) {
            try {
                String connectionId = removeFlowRule(session, r);
                getConnectionCache().remove(did(), connectionId);
                removed.add(r);
            } catch (Exception e) {
                openConfigError("Error {}", e);
                continue;
            }
        }
        openConfigLog("removedFlowRules removed {}", removed.size());
        return removed;
    }

    private DeviceConnectionCache getConnectionCache() {
        return DeviceConnectionCache.init();
    }

    /**
     * Helper method to get the device id.
     */
    private DeviceId did() {
        return data().deviceId();
    }

    /**
     * Helper method to log from this class adding DeviceId.
     */
    private void openConfigLog(String format, Object... arguments) {
        log.info("OPENCONFIG {}: " + format, did(), arguments);
    }

    /**
     * Helper method to log an error from this class adding DeviceId.
     */
    private void openConfigError(String format, Object... arguments) {
        log.error("OPENCONFIG {}: " + format, did(), arguments);
    }


    /**
     * Helper method to get the Netconf Session.
     */
    private NetconfSession getNetconfSession() {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        return controller.getNetconfDevice(did()).getSession();
    }

    private void setOpticalChannelFrequency(NetconfSession session,
                                            String optChannel, Frequency freq)
            throws NetconfException {
        StringBuilder sb = new StringBuilder();
        sb.append(
                    "<components xmlns='http://openconfig.net/yang/platform'>"
                  + "<component>"
                  + "<config>"
                  + "<name>" + optChannel + "</name>"
                  + "</config>"
                  + "<optical-channel xmlns='http://openconfig.net/yang/terminal-device'>"
                  + "<config>"
                  + "<frequency>" + (long) freq.asMHz() + "</frequency>"
                  + "</config>"
                  + "</optical-channel>"
                  + "</component>"
                  + "</components>");
        log.info("Optical Channel Frequency {}", sb.toString());
        boolean ok = session.editConfig(DatastoreId.RUNNING, null, sb.toString());
        if (!ok) {
            throw new NetconfException("error writing channel frequency");
        }
        ok = session.commit();
        if (!ok) {
            throw new NetconfException("error committing channel frequency");
        }
    }

    /**
     * Get the OpenConfig component name for the OpticalChannel component.
     *
     * @param portNumber ONOS port number of the Line port ().
     * @return the channel component name or null
     */
    private String getOpticalChannel(PortNumber portNumber) {
        Port clientPort = handler().get(DeviceService.class).getPort(did(), portNumber);
        return clientPort.annotations().value(OC_NAME);
    }

    /**
     * Apply the flowrule.
     * <p>
     * Note: only bidirectional are supported as of now,
     * given OpenConfig note (below). In consequence, only the
     * TX rules are actually mapped to netconf ops.
     * <p>
     * https://github.com/openconfig/public/blob/master/release/models
     * /optical-transport/openconfig-terminal-device.yang
     * <p>
     * Directionality:
     * To maintain simplicity in the model, the configuration is
     * described from client-to-line direction.  The assumption is that
     * equivalent reverse configuration is implicit, resulting in
     * the same line-to-client configuration.
     *
     * @param session The Netconf session.
     * @param r       Flow Rules to be applied.
     * @return the optical channel + the frequency or just channel as identifier fo the config installed on the device
     * @throws NetconfException if exchange goes wrong
     */
    protected String applyFlowRule(NetconfSession session, FlowRule r) throws NetconfException {
        FlowRuleParser frp = new FlowRuleParser(r);
        if (!frp.isReceiver()) {
            String optChannel = getOpticalChannel(frp.getPortNumber());
            setOpticalChannelFrequency(session, optChannel,
                    frp.getCentralFrequency());
            return optChannel + ":" + frp.getCentralFrequency().asGHz();
        }
        return String.valueOf(frp.getCentralFrequency().asGHz());
    }


    protected String removeFlowRule(NetconfSession session, FlowRule r)
            throws NetconfException {
        FlowRuleParser frp = new FlowRuleParser(r);
        if (!frp.isReceiver()) {
            String optChannel = getOpticalChannel(frp.getPortNumber());
            setOpticalChannelFrequency(session, optChannel, Frequency.ofMHz(0));
            return optChannel + ":" + frp.getCentralFrequency().asGHz();
        }
        return String.valueOf(frp.getCentralFrequency().asGHz());
    }
}
