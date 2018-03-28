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

import org.onosproject.yang.gen.v1.opticalswitch.rev20180322.opticalswitch.CrossConnects;
import org.onosproject.yang.gen.v1.opticalswitch.rev20180322.opticalswitch.crossconnects.Pair;

import com.google.common.collect.ImmutableList;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.netconf.NetconfException;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.netconfGet;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.netconfEditConfig;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.opticalRevision;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.configsAt;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.xmlOpen;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.xmlClose;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_CONNS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_CONNS_XMLNS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_DATA_CONNS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.CFG_MODE_MERGE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Flow rule programmable behaviour for polatis optical netconf devices.
 */
public class PolatisFlowRuleProgrammable
    extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    public static final String KEY_CHID = "wavelength-id";
    public static final String KEY_SRC = "ingress";
    public static final String KEY_DST = "egress";
    public static final String KEY_SRC_CHID = String.format("%s.%s", KEY_SRC, KEY_CHID);
    public static final String CFG_MODE_DELETE = "delete";
    public static final String KEY_PAIR = "pair";
    public static final String KEY_PAIRS = "pairs";
    public static final String KEY_PAIR_DELETE = String.format("%s %s", KEY_PAIR, CFG_MODE_DELETE);
    public static final String PAIR_COMPAT_REVISION = "2017-08-04";

    private static final Logger log = getLogger(PolatisFlowRuleProgrammable.class);

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
        return new StringBuilder(xmlOpen(KEY_CONNS_XMLNS))
                .append(xmlClose(KEY_CONNS))
                .toString();
    }

    private Collection<FlowEntry> parseConnections() {
        log.debug("Fetch connections...");
        String reply = netconfGet(handler(), getConnectionsFilter());
        final String keyPairMode = String.format("%s.%s", KEY_DATA_CONNS, parseKeyPairCompat());
        List<HierarchicalConfiguration> subtrees = configsAt(reply, keyPairMode);
        ImmutableList.Builder<FlowEntry> connectionsBuilder = ImmutableList.builder();
        for (HierarchicalConfiguration connection : subtrees) {
            connectionsBuilder.add(new DefaultFlowEntry(parseConnection(connection), FlowEntry.FlowEntryState.ADDED));
        }
        return connectionsBuilder.build();
    }

    private FlowRule parseConnection(HierarchicalConfiguration cfg) {
        return PolatisOpticalUtility.toFlowRule(this,
                PortNumber.portNumber(cfg.getInt(KEY_SRC)),
                PortNumber.portNumber(cfg.getInt(KEY_DST)));
    }

    private Collection<FlowRule> applyConnections(Collection<FlowRule> rules) {
        return rules.stream()
                .filter(c -> editConnection(c, CFG_MODE_MERGE))
                .collect(Collectors.toList());
    }

    private boolean editConnection(FlowRule rule, String mode) {
        CrossConnects crossConnects = PolatisOpticalUtility.fromFlowRule(this, rule);
        final StringBuilder cfg = new StringBuilder(xmlOpen(KEY_CONNS_XMLNS));
        List<Pair> pairs = crossConnects.pair();
        final String keyPairCompat = parseKeyPairCompat();
        final String keyPairMode = String.format("%s operation=\"%s\"", keyPairCompat, mode);
        pairs.forEach(p -> {
                cfg.append(xmlOpen(keyPairMode))
                .append(xmlOpen(KEY_SRC))
                .append(p.ingress())
                .append(xmlClose(KEY_SRC))
                .append(xmlOpen(KEY_DST))
                .append(p.egress())
                .append(xmlClose(KEY_DST))
                .append(xmlClose(keyPairCompat));
        });
        cfg.append(xmlClose(KEY_CONNS));
        return netconfEditConfig(handler(), null, cfg.toString());
    }

    private Collection<FlowRule> removeConnections(Collection<FlowRule> rules) {
        return rules.stream()
                .filter(c -> editConnection(c, CFG_MODE_DELETE))
                .collect(Collectors.toList());
    }

    private String parseKeyPairCompat() {
        String rev = opticalRevision(handler());
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
