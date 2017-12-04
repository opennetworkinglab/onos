/*
 * Copyright 2016 Open Networking Foundation
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

package org.onosproject.drivers.oplink;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.drivers.oplink.OplinkNetconfUtility.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Flow rule programmable behaviour for oplink optical netconf devices.
 */
public class OplinkOpticalFlowRuleProgrammable
        extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    // key
    public static final String KEY_CHID = "wavelength-id";
    public static final String KEY_SRC = "source";
    public static final String KEY_DST = "destination";
    public static final String KEY_SRC_PORTID = String.format("%s.%s", KEY_SRC, KEY_PORTID);
    public static final String KEY_SRC_CHID = String.format("%s.%s", KEY_SRC, KEY_CHID);
    public static final String KEY_DST_PORTID = String.format("%s.%s", KEY_DST, KEY_PORTID);

    // log
    private static final Logger log = getLogger(OplinkOpticalFlowRuleProgrammable.class);

    @Override
    public Collection<FlowEntry> getFlowEntries() {
        return parseConnections();
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        return applyConnections(rules);
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        return removeConnections(rules);
    }

    private String getConnectionsFilter() {
        return new StringBuilder(xmlOpen(KEY_OPENOPTICALDEV_XMLNS))
                .append(xmlEmpty(KEY_CONNS))
                .append(xmlClose(KEY_OPENOPTICALDEV))
                .toString();
    }

    private Collection<FlowEntry> parseConnections() {
        log.debug("Fetch connections...");
        String reply = netconfGet(handler(), getConnectionsFilter());
        List<HierarchicalConfiguration> subtrees = configsAt(reply, KEY_DATA_CONNS);
        Collection<FlowEntry> list = new ArrayList<>();
        for (HierarchicalConfiguration connection : subtrees) {
            list.add(new DefaultFlowEntry(parseConnection(connection), FlowEntry.FlowEntryState.ADDED));
        }
        return list;
    }

    private FlowRule parseConnection(HierarchicalConfiguration cfg) {
        return OplinkOpticalUtility.toFlowRule(this,
                                               PortNumber.portNumber(cfg.getString(KEY_SRC_PORTID)),
                                               PortNumber.portNumber(cfg.getString(KEY_DST_PORTID)),
                                               cfg.getInt(KEY_SRC_CHID));

    }

    private Collection<FlowRule> applyConnections(Collection<FlowRule> rules) {
        return rules.stream()
                .filter(c -> applyConnection(c))
                .collect(Collectors.toList());
    }

    private boolean applyConnection(FlowRule rule) {
        log.debug("Applying connection {}", rule);
        OplinkCrossConnect crossConnect = OplinkOpticalUtility.fromFlowRule(this, rule);
        // Build xml
        String connID = Integer.toString(crossConnect.getChannel());
        String cfg = new StringBuilder(xmlOpen(KEY_OPENOPTICALDEV_XMLNS))
                .append(xmlOpen(KEY_CONNS))
                .append(xml(KEY_CONNID, connID))
                .append(xmlOpen(KEY_SRC))
                .append(xml(KEY_PORTID, crossConnect.getInPort().name()))
                .append(xml(KEY_CHID, connID))
                .append(xmlClose(KEY_SRC))
                .append(xmlOpen(KEY_DST))
                .append(xml(KEY_PORTID, crossConnect.getOutPort().name()))
                .append(xmlClose(KEY_DST))
                .append(xml(KEY_CHATT, Integer.toString(crossConnect.getAttenuation())))
                .append(xmlClose(KEY_CONNS))
                .append(xmlClose(KEY_OPENOPTICALDEV))
                .toString();
        return netconfEditConfig(handler(), CFG_MODE_MERGE, cfg);
    }

    private Collection<FlowRule> removeConnections(Collection<FlowRule> rules) {
        return rules.stream()
                .filter(c -> removeConnection(c))
                .collect(Collectors.toList());
    }

    private boolean removeConnection(FlowRule rule) {
        log.debug("Removing connection {}", rule);
        OplinkCrossConnect crossConnect = OplinkOpticalUtility.fromFlowRule(this, rule);
        // Build xml
        String connID = Integer.toString(crossConnect.getChannel());
        String cfg = new StringBuilder(xmlOpen(KEY_OPENOPTICALDEV_XMLNS))
                .append(xmlOpen(String.format("%s %s", KEY_CONNS, CFG_OPT_DELETE)))
                .append(xml(KEY_CONNID, connID))
                .append(xmlOpen(KEY_SRC))
                .append(xml(KEY_PORTID, crossConnect.getInPort().name()))
                .append(xml(KEY_CHID, connID))
                .append(xmlClose(KEY_SRC))
                .append(xmlClose(KEY_CONNS))
                .append(xmlClose(KEY_OPENOPTICALDEV))
                .toString();
        return netconfEditConfig(handler(), CFG_MODE_NONE, cfg);
    }
}
