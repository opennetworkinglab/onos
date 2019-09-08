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

import javax.xml.namespace.NamespaceContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_NAME;

/**
 * Implementation of FlowRuleProgrammable interface for
 * OpenConfig terminal devices.
 */
public class GrooveOpenConfigFlowRuleProgrammable
        extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    private static final Logger log =
            LoggerFactory.getLogger(GrooveOpenConfigFlowRuleProgrammable.class);

    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";


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

    // Context so XPath expressions are aware of XML namespaces
    private static final NamespaceContext NS_CONTEXT = new NamespaceContext() {
        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix.equals("oc-platform-types")) {
                return "http://openconfig.net/yang/platform-types";
            }
            if (prefix.equals("oc-opt-term")) {
                return "http://openconfig.net/yang/terminal-device";
            }
            return null;
        }

        @Override
        public Iterator getPrefixes(String val) {
            return null;
        }

        @Override
        public String getPrefix(String uri) {
            return null;
        }
    };


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


    /**
     * Construct a String with a Netconf filtered get RPC Message.
     *
     * @param filter A valid XML tree with the filter to apply in the get
     * @return a String containing the RPC XML Document
     */
    private String filteredGetBuilder(String filter) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='subtree'>");
        rpc.append(filter);
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }

    /**
     * Construct a get request to retrieve Components and their
     * properties (for the ONOS port, index).
     *
     * @return The filt content to send to the device.
     */
    private String getComponents() {
        StringBuilder filt = new StringBuilder();
        filt.append("<components xmlns='http://openconfig.net/yang/platform'>");
        filt.append(" <component>");
        filt.append("  <name/>");
        filt.append("  <properties/>");
        filt.append(" </component>");
        filt.append("</components>");
        return filteredGetBuilder(filt.toString());
    }


    /**
     * Construct a get request to retrieve Optical Channels and
     * the line port they are using.
     * <p>
     * Thset ort.
     *
     * @return The filt content to send to the device.
     */
    private String getOpticalChannels() {
        StringBuilder filt = new StringBuilder();
        filt.append("<components xmlns='http://openconfig.net/yang/platform'>");
        filt.append(" <component>");
        filt.append("  <name/>");
        filt.append("  <state/>");
        filt.append("  <oc-opt-term:optical-channel xmlns:oc-opt-term"
                            + " = 'http://openconfig.net/yang/terminal-device'>");
        filt.append("    <oc-opt-term:config>");
        filt.append("     <oc-opt-term:line-port/>");
        filt.append("    </oc-opt-term:config >");
        filt.append("  </oc-opt-term:optical-channel>");
        filt.append(" </component>");
        filt.append("</components>");
        return filteredGetBuilder(filt.toString());
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


    private void setOpticalChannelFrequency(NetconfSession session, StringBuilder sb) throws NetconfException {

        final boolean ok =
                session.editConfig(DatastoreId.RUNNING, null, sb.toString());

        if (!ok) {
            throw new NetconfException("error writing channel frequency");
        }
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
            final long frequency = (long) frp.getCentralFrequency().asMHz();
            final String transceiver = optChannel.replace("OCH", "TRANSCEIVER");
            final StringBuilder sb = new StringBuilder();
            sb.append(
                    "<components xmlns='http://openconfig.net/yang/platform'>"
                            + "<component>"
                            + "<name>" + optChannel + "</name>"
                            + "<oc-opt-term:optical-channel "
                            +
                            "    xmlns:oc-opt-term='http://openconfig.net/yang/terminal-device'>"
                            + "  <oc-opt-term:config>"
                            + "   <oc-opt-term:frequency>" + frequency + "</oc-opt-term:frequency>"
                            + "  </oc-opt-term:config>"
                            + " </oc-opt-term:optical-channel>"
                            + "</component>"
                            + "<component>"
                            + "  <name>" + transceiver + "</name>"
                            + "   <transceiver xmlns='http://openconfig.net/yang/platform/transceiver'>"
                            + "     <config>"
                            + "       <enabled>true</enabled>"
                            + "     </config>"
                            + "   </transceiver>"
                            + " </component>"
                            + "</components>");

            setOpticalChannelFrequency(session, sb);
            return optChannel + ":" + frp.getCentralFrequency().asGHz();
        }
        return String.valueOf(frp.getCentralFrequency().asGHz());
    }

    protected String removeFlowRule(NetconfSession session, FlowRule r)
            throws NetconfException {
        FlowRuleParser frp = new FlowRuleParser(r);
        if (!frp.isReceiver()) {
            String optChannel = getOpticalChannel(frp.getPortNumber());
            final String transceiver = optChannel.replace("OCH", "TRANSCEIVER");
            final StringBuilder sb = new StringBuilder();
            /*
            TODO imcomplete solution: doesn't set frequency to zero; see below
            sb.append(
                    "<components xmlns='http://openconfig.net/yang/platform'>"
                            + "<component>"
                            + "  <name>" + transceiver + "</name>"
                            + "   <transceiver xmlns='http://openconfig.net/yang/platform/transceiver'>"
                            + "     <config>"
                            + "       <enabled>false</enabled>"
                            + "     </config>"
                            + "   </transceiver>"
                            + " </component>"
                            + "</components>");

             */

            // NOTE: Disabling the laser via openconfig on Groove is not possible
            //       and consequently the frequency can't be set to 0.
            //       This is an error on mapping between native and openconfig model.

            // Workaround: use legacy model
            Pattern transceiverPattern = Pattern.compile(".*-[1]-[1-9][0-4]?-L[1-2]$"); // e.g. TRANSCEIVER-1-1-L1
            Matcher lineMatch = transceiverPattern.matcher(transceiver);
            lineMatch.find();
            final String[] split = lineMatch.group(0).split("-");

            sb.append("<ne xmlns=\"http://coriant.com/yang/os/ne\">" +
                              "        <shelf>" +
                              "          <shelf-id>" + split[1] + "</shelf-id>" +
                              "          <slot>" +
                              "            <slot-id>" + split[2] + "</slot-id>" +
                              "            <card>" +
                              "              <port>" +
                              "                <port-id>" + split[3].replace("L", "") + "</port-id>" +
                              "                <och-os>" +
                              "                  <frequency>0</frequency>" +
                              "                  <laser-enable>disabled</laser-enable>" +
                              "                </och-os>" +
                              "              </port>" +
                              "            </card>" +
                              "          </slot>" +
                              "        </shelf>" +
                              "      </ne>");

            setOpticalChannelFrequency(session, sb);
            return optChannel + ":" + frp.getCentralFrequency().asGHz();
        }
        return String.valueOf(frp.getCentralFrequency().asGHz());
    }
}
