/*
 * Copyright 2018 Open Networking Foundation
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

package org.onosproject.drivers.polatis.snmp;

import com.google.common.collect.ImmutableList;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TableEvent;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.drivers.polatis.snmp.PolatisOpticalUtility.fromFlowRule;
import static org.onosproject.drivers.polatis.snmp.PolatisSnmpUtility.getTable;
import static org.onosproject.drivers.polatis.snmp.PolatisSnmpUtility.set;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Flow rule programmable behaviour for Polatis optical snmp devices.
 */
public class PolatisFlowRuleProgrammable
    extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    private static final String PORT_ENTRY_OID = ".1.3.6.1.4.1.26592.2.2.2.1.2";
    private static final String PORT_PATCH_OID = PORT_ENTRY_OID + ".1.2";

    private final Logger log = getLogger(getClass());

    @Override
    public Collection<FlowEntry> getFlowEntries() {
        List<TableEvent> events;
        DeviceId deviceId = handler().data().deviceId();
        ImmutableList.Builder<FlowEntry> connectionsBuilder = ImmutableList.builder();

        try {
            OID[] columnOIDs = {new OID(PORT_PATCH_OID)};
            events = getTable(handler(), columnOIDs);
        } catch (IOException e) {
            log.error("Error reading ports table for device {} exception {}", deviceId, e);
            return connectionsBuilder.build();
        }

        if (events == null) {
            log.error("Error reading ports table for device {}", deviceId);
            return connectionsBuilder.build();
        }

        for (TableEvent event : events) {
            if (event == null) {
                log.error("Error reading event for device {}", deviceId);
                continue;
            }
            VariableBinding[] columns = event.getColumns();
            if (columns == null) {
                log.error("Error reading columns for device {}", deviceId);
                continue;
            }

            VariableBinding patchColumn = columns[0];
            if (patchColumn == null) {
                continue;
            }

            int port = event.getIndex().last();
            int patch = patchColumn.getVariable().toInt();
            if (patch == 0) {
                continue;
            }

            FlowRule flowRule = PolatisOpticalUtility.toFlowRule(this,
                PortNumber.portNumber(port), PortNumber.portNumber(patch));
            connectionsBuilder.add(new DefaultFlowEntry(flowRule, FlowEntry.FlowEntryState.ADDED));
        }

        return connectionsBuilder.build();
    }

    private boolean editConnection(FlowRule rule, boolean delete) {
        List<VariableBinding> vbs = new ArrayList<>();
        vbs.add(fromFlowRule(rule, delete));
        DeviceId deviceId = handler().data().deviceId();
        try {
            set(handler(), vbs);
        } catch (IOException e) {
            log.error("Error writing ports table for device {} exception {}", deviceId, e);
            return false;
        }
        // TODO: check for errors
        return true;
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        return rules.stream()
                .filter(c -> editConnection(c, false))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        return rules.stream()
                .filter(c -> editConnection(c, true))
                .collect(Collectors.toList());
    }
}
