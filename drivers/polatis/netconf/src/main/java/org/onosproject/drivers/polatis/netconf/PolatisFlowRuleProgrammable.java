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

import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.drivers.polatis.netconf.PolatisUtility.parseKeyPairCompat;
import static org.onosproject.drivers.polatis.netconf.PolatisUtility.parseConnections;
import static org.onosproject.drivers.polatis.netconf.PolatisUtility.KEY_SRC;
import static org.onosproject.drivers.polatis.netconf.PolatisUtility.KEY_DST;

import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.netconfEditConfig;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.xmlOpen;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.xmlClose;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_CONNS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_CONNS_XMLNS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.CFG_MODE_MERGE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Flow rule programmable behaviour for Polatis optical netconf devices.
 */
public class PolatisFlowRuleProgrammable
    extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    public static final String KEY_CHID = "wavelength-id";
    public static final String KEY_SRC_CHID = String.format("%s.%s", KEY_SRC, KEY_CHID);
    public static final String CFG_MODE_DELETE = "delete";

    private static final Logger log = getLogger(PolatisFlowRuleProgrammable.class);

    @Override
    public Collection<FlowEntry> getFlowEntries() {
        return parseConnections(this);
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        return applyConnections(rules);
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        return removeConnections(rules);
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
        final String keyPairCompat = parseKeyPairCompat(this);
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

}
