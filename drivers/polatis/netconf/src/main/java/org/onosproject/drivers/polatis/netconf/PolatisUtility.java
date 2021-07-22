/*
 * Copyright 2017 Open Networking Foundation
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

package org.onosproject.drivers.polatis.netconf;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.collect.ImmutableList;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.netconf.NetconfException;

import static org.onosproject.net.optical.device.OmsPortHelper.omsPortDescription;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.configsAt;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.netconfGet;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.xmlOpen;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.xml;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.xmlClose;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.xmlEmpty;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.opticalRevision;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PAIR;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PAIRS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORT;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORTID;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORTPEER;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORTDIR;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORTCONFIG;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PRODINF;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_CONNS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORTSETSTATE;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORTCONFIG_XMLNS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PRODINF_XMLNS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_CONNS_XMLNS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORTSETSTATE_XMLNS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_DATA_PORTCONFIG;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_DATA_CONNS;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;


/**
 * Polatis common utilities.
 */
public final class PolatisUtility {

    public static final String DEFAULT_MANUFACTURER = "Polatis";
    public static final String DEFAULT_DESCRIPTION_DATA = "Unknown";
    public static final String KEY_MANUFACTURER = "manufacturer";
    public static final String KEY_HWVERSION = "model-name";
    public static final String KEY_SWVERSION = "software-version";
    public static final String KEY_SERIALNUMBER = "serial-number";
    public static final String KEY_INPUTPORTS = "inputPorts";
    public static final String KEY_OUTPUTPORTS = "outputPorts";
    public static final String KEY_PORTSTATUS = "status";
    public static final String KEY_ENABLE = "ENABLE";
    public static final String KEY_DISABLE = "DISABLE";
    public static final String KEY_INGRESS = "ingress";
    public static final String KEY_EGRESS = "egress";
    public static final String VALUE_TRUE = "true";
    public static final String VALUE_FALSE = "false";
    public static final String VALUE_INPUT = "INPUT";
    public static final String VALUE_OUTPUT = "OUTPUT";
    public static final String VALUE_UNKNOWN = "UNKNOWN";
    public static final String VALUE_CC = "CC";
    public static final String KEY_RPCENABLE = "port-enab";
    public static final String KEY_RPCDISABLE = "port-disab";
    public static final String PORT_ENABLED = "ENABLED";
    public static final String PORT_DISABLED = "DISABLED";
    public static final String KEY_PORTLABEL = "label";
    public static final String KEY_PEERPORT = "peer-port";
    public static final String KEY_LINKBIDIR = "bidirectional";
    public static final String KEY_LINKALLOWED = "allowed";
    public static final String KEY_SRC = "ingress";
    public static final String KEY_DST = "egress";
    public static final String PAIR_COMPAT_REVISION = "2017-08-04";

    public static final int POLATIS_NUM_OF_WAVELENGTHS = 39;

    private static final Logger log = getLogger(PolatisDeviceDescription.class);

    private PolatisUtility() {
    }

    /**
     * Returns XML subtree filter term for NETCONF get[-config] RPC for retrieving product info from a Polatis switch.
     *
     * @return Filter term as string
     */
    public static String getProdInfoFilter() {
        return new StringBuilder(xmlOpen(KEY_PRODINF_XMLNS))
            .append(xmlClose(KEY_PRODINF))
            .toString();
    }

    /**
     * Returns XML subtree filter term for NETCONF get[-config] RPC for retrieving cross-connections on a
     * Polatis switch.
     *
     * @return Filter term as string
     */
    public static String getConnectionsFilter() {
        return new StringBuilder(xmlOpen(KEY_CONNS_XMLNS))
                .append(xmlClose(KEY_CONNS))
                .toString();
    }

    /**
     * Returns XML subtree filter term for NETCONF get[-config] RPC for retrieving config and state data for all ports
     * on a Polatis switch.
     *
     * @return Filter term as string
     */
    public static String getPortsFilter() {
        return new StringBuilder(xmlOpen(KEY_PORTCONFIG_XMLNS))
            .append(xmlOpen(KEY_PORT))
            .append(xmlEmpty(KEY_PORTID))
            .append(xmlEmpty(KEY_PORTSTATUS))
            .append(xmlEmpty(KEY_PORTLABEL))
            .append(xmlEmpty(KEY_PEERPORT))
            .append(xmlClose(KEY_PORT))
            .append(xmlClose(KEY_PORTCONFIG))
            .toString();
    }

    /**
     * Returns XML subtree filter term for NETCONF get[-config] RPC for retrieving config and state data for a specific
     * port on a Polatis switch.
     *
     * @param portNum Port number as PortNumber object
     * @return        Filter term as string
     */
    public static String getPortFilter(PortNumber portNum) {
        return new StringBuilder(xmlOpen(KEY_PORTCONFIG_XMLNS))
            .append(xmlOpen(KEY_PORT))
            .append(xml(KEY_PORTID, portNum.toString()))
            .append(xmlEmpty(KEY_PORTSTATUS))
            .append(xmlEmpty(KEY_PORTLABEL))
            .append(xmlEmpty(KEY_PEERPORT))
            .append(xmlClose(KEY_PORT))
            .append(xmlClose(KEY_PORTCONFIG))
            .toString();
    }

    /**
     * Returns XML subtree filter term for NETCONF get RPC for retrieving state data (only) for a specific port on a
     * Polatis switch.
     *
     * @param portNum Port number as PortNumber object
     * @return        Filter term as string
     */
    public static String getPortStatusFilter(PortNumber portNum) {
        return new StringBuilder(xmlOpen(KEY_PORTCONFIG_XMLNS))
            .append(xmlOpen(KEY_PORT))
            .append(xml(KEY_PORTID, portNum.toString()))
            .append(xmlEmpty(KEY_PORTSTATUS))
            .append(xmlClose(KEY_PORT))
            .append(xmlClose(KEY_PORTCONFIG))
            .toString();
    }

     /**
     * Returns XML body for NETCONF RPC for setting the admin status of a specific port on a Polatis switch.
     *
     * @param action  Action (enable/disable) to be performed on port as string
     * @param portNum Port number as PortNumber object
     * @return        RPC body (XML) as string
     */
    public static String getRpcSetPortStateBody(String action, PortNumber portNum) {
        return new StringBuilder(xmlOpen(KEY_PORTSETSTATE_XMLNS))
            .append(xmlOpen(action.equals(KEY_ENABLE) ? KEY_RPCENABLE : KEY_RPCDISABLE))
            .append(portNum.toString())
            .append(xmlClose(action.equals(KEY_ENABLE) ? KEY_RPCENABLE : KEY_RPCDISABLE))
            .append(xmlClose(KEY_PORTSETSTATE))
            .toString();
    }

     /**
     * Returns a list of PortDescriptions from parsing the content of the reply to a get[-config] call to a
     * Polatis switch.
     *
     * @param content        XML to be parsed as string
     * @param numInputPorts  Number of input ports
     * @param numOutputPorts Number of output ports
     * @return               List of ports as PortDescription objects
     */
    public static List<PortDescription> parsePorts(String content, int numInputPorts, int numOutputPorts) {
        List<HierarchicalConfiguration> subtrees = configsAt(content, KEY_DATA_PORTCONFIG);
        List<PortDescription> portDescriptions = new ArrayList<PortDescription>();
        for (HierarchicalConfiguration portConfig : subtrees) {
            PortDescription parsedPort = parsePort(portConfig, numInputPorts, numOutputPorts == 0 ? true : false);
            portDescriptions.add(parsedPort);
        }
        return portDescriptions;
    }

    /**
     * Returns a single PortDescription from parsing a HierarchicalConfiguration object containing a Polatis switch
     * port config.
     *
     * @param cfg            Single port as HierarchicalConfiguration object
     * @param numInputPorts  Number of input ports
     * @param isConfigurable Switch is CC
     * @return               Single port as PortDescription object
     */
    public static PortDescription parsePort(HierarchicalConfiguration cfg, int numInputPorts, boolean isConfigurable) {
        PortNumber portNumber = PortNumber.portNumber(cfg.getLong(KEY_PORTID));
        String portType = VALUE_UNKNOWN;
        if (isConfigurable) {
            portType = VALUE_CC;
        } else {
            portType = portNumber.toLong() > numInputPorts ? VALUE_OUTPUT : VALUE_INPUT;
        }
        String peerPort = cfg.getString(KEY_PEERPORT);
        DefaultAnnotations annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, cfg.getString(KEY_PORTLABEL))
                .set(KEY_PORTPEER, cfg.getString(KEY_PEERPORT))
                .set(KEY_PORTDIR, portType)
                .build();
        return omsPortDescription(portNumber,
                cfg.getString(KEY_PORTSTATUS).equals(PORT_ENABLED),
                Spectrum.U_BAND_MIN, Spectrum.O_BAND_MAX,
                Frequency.ofGHz(6_25), annotations);
    }

    /**
     * Returns flow entries representing current cross-connections on a Polatis optical switch.
     *
     * @param behaviour HandlerBehaviour object associated with device being queried
     * @return          Cross-connections as a collection of FlowEntry objects
     */
    public static Collection<FlowEntry> parseConnections(HandlerBehaviour behaviour) {
        log.debug("Fetch connections...");
        String reply = netconfGet(behaviour.handler(), getConnectionsFilter());
        final String keyPairMode = String.format("%s.%s", KEY_DATA_CONNS, parseKeyPairCompat(behaviour));
        List<HierarchicalConfiguration> subtrees = configsAt(reply, keyPairMode);
        ImmutableList.Builder<FlowEntry> connectionsBuilder = ImmutableList.builder();
        for (HierarchicalConfiguration connection : subtrees) {
            connectionsBuilder.add(new DefaultFlowEntry(parseConnection(connection, behaviour),
                                   FlowEntry.FlowEntryState.ADDED));
        }
        return connectionsBuilder.build();
    }

    /**
     * Returns single cross-connection as FlowRule object from parsing HierarchicalConfiguration object.
     *
     * @param cfg       Single cross-connection as XML encoded in HierarchicalConfiguration object
     * @param behaviour HandlerBehaviour object associated with device from which cross-connection has been retrieved
     * @return          Cross-connection as a FlowEntry object
     */
    public static FlowRule parseConnection(HierarchicalConfiguration cfg, HandlerBehaviour behaviour) {
        return PolatisOpticalUtility.toFlowRule(behaviour,
                PortNumber.portNumber(cfg.getInt(KEY_SRC)),
                PortNumber.portNumber(cfg.getInt(KEY_DST)));
    }

    /**
     * Returns string containing the correct name of the YANG list node containing the cross-connections on a specific
     * Polatis optical switch.
     * <p>
     * This handles the backwards incompatible change ('pairs' to 'pair') introduced in the Polatis optical-switch
     * YANG module in revision 2017-08-04
     *
     * @param behaviour HandlerBehaviour object associated with the ONOS device representing a particular Polatis
     *                  optical switch
     * @return          Correct YANG list node name as string
     */
    public static String parseKeyPairCompat(HandlerBehaviour behaviour) {
        String rev = opticalRevision(behaviour.handler());
        if (rev == null) {
            throw new IllegalStateException(new NetconfException("Failed to obtain the revision."));
        }
        String keyPairCompat;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(PAIR_COMPAT_REVISION);

            if (date.compareTo(sdf.parse(rev)) > 0) {
                keyPairCompat = KEY_PAIRS;
            } else {
                keyPairCompat = KEY_PAIR;
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException(new NetconfException(String.format("Incorrect date format: %s", rev)));
        }
        return keyPairCompat;
    }
}
