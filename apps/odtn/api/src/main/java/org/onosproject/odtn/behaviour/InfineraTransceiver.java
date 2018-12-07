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
 */
package org.onosproject.odtn.behaviour;

import com.google.common.base.Strings;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.odtn.behaviour.OdtnTerminalDeviceDriver.Operation;
import org.onosproject.odtn.utils.openconfig.OpenConfigAssignmentHandler;
import org.onosproject.odtn.utils.openconfig.OpenConfigChannelHandler;
import org.onosproject.odtn.utils.openconfig.OpenConfigConfigOfAssignmentHandler;
import org.onosproject.odtn.utils.openconfig.OpenConfigConfigOfChannelHandler;
import org.onosproject.odtn.utils.openconfig.OpenConfigLogicalChannelAssignmentsHandler;
import org.onosproject.odtn.utils.openconfig.OpenConfigLogicalChannelsHandler;
import org.onosproject.odtn.utils.openconfig.OpenConfigTerminalDeviceHandler;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanassignmentconfig.AssignmentTypeEnum;

import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility class for NETCONF driver.
 */
public class InfineraTransceiver extends AbstractHandlerBehaviour
        implements ConfigurableTransceiver {

    private final Logger log = getLogger(getClass());

    private static final String ANOTATION_NAME   = "xc:operation";

    @Override
    public List<CharSequence> enable(PortNumber client, PortNumber line, boolean enable) {

        log.debug("enable() infinera route");
        DeviceId did = this.data().deviceId();
        Port clientPort = handler().get(DeviceService.class).getPort(did, client);
        if (clientPort == null) {
            log.warn("{} does not exist on {}", client, did);
            return Collections.emptyList();
        }
        String clientName = clientPort.annotations().value(OC_NAME);
        if (Strings.isNullOrEmpty(clientName)) {
            log.warn("{} annotations not exist on {}@{}", OC_NAME, client, did);
            return Collections.emptyList();
        }

        Port linePort = handler().get(DeviceService.class).getPort(did, line);
        if (linePort == null) {
            log.warn("{} does not exist on {}", line, did);
            return Collections.emptyList();
        }
        String lineName = linePort.annotations().value(OC_NAME);
        if (Strings.isNullOrEmpty(lineName)) {
            log.warn("{} annotations not exist on {}@{}", OC_NAME, line, did);
            return Collections.emptyList();
        }

        // create <terminal-device xmlns="http://openconfig.net/yang/terminal-device">
        //        </terminal-device>
        OpenConfigTerminalDeviceHandler terminalDevice = new OpenConfigTerminalDeviceHandler();

        // add <logical-channels></logical-channels>
        OpenConfigLogicalChannelsHandler logicalChannels =
            new OpenConfigLogicalChannelsHandler(terminalDevice);

        // add <channel><index>"clientName"</index></channel>
        OpenConfigChannelHandler channel =
            new OpenConfigChannelHandler(Integer.parseInt(clientName), logicalChannels);

        // add <config><index>"clientName"</index></config>
        OpenConfigConfigOfChannelHandler configOfChannel =
            new OpenConfigConfigOfChannelHandler(channel);
        configOfChannel.addIndex(Integer.parseInt(clientName));

        // add <logical-channel-assignments xc:operation="merge/delete">
        OpenConfigLogicalChannelAssignmentsHandler logicalChannelAssignments =
            new OpenConfigLogicalChannelAssignmentsHandler(channel);
        if (enable) {
            logicalChannelAssignments.addAnnotation(ANOTATION_NAME, Operation.MERGE.value());
        } else {
            logicalChannelAssignments.addAnnotation(ANOTATION_NAME, Operation.DELETE.value());
        }

        // add <assignment><index>"clientName"</index></assignment>
        OpenConfigAssignmentHandler assignment =
            new OpenConfigAssignmentHandler(Integer.parseInt(clientName), logicalChannelAssignments);

        // add <config><assignment-type>LOGICAL_CHANNEL</assignment-type>
        //             <logical-channel>"lineName"</logical-channel>
        //             <allocation>100</allocation>
        //     </config>
        OpenConfigConfigOfAssignmentHandler configOfAssignment =
            new OpenConfigConfigOfAssignmentHandler(assignment);
        configOfAssignment.addAssignmentType(AssignmentTypeEnum.LOGICAL_CHANNEL);
        configOfAssignment.addLogicalChannel(lineName);
        configOfAssignment.addAllocation(BigDecimal.valueOf(100));

        return terminalDevice.getListCharSequence();
    }
}
