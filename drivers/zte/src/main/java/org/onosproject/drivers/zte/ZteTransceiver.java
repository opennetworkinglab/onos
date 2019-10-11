/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.drivers.zte;

import com.google.common.base.Strings;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.odtn.behaviour.ConfigurableTransceiver;
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

import java.util.Collections;
import java.util.List;

import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_NAME;
import static org.slf4j.LoggerFactory.getLogger;

public class ZteTransceiver extends AbstractHandlerBehaviour
        implements ConfigurableTransceiver {

    private final Logger log = getLogger(getClass());

    private static final String ANOTATION_NAME = "xc:operation";

    private final int[] clientPortIndexs = new int[] {
            20975681, 18878529, 19927105, 17829953,
            20975745, 18878593, 19927169, 17830017,
            20975809, 18878657, 19927233, 17830081,
            20975873, 18878721, 19927297, 17830145
    };

    private final int[] linePortIndexs = new int[] {
            21499969, 19402817, 20451393, 18354241,
            21500033, 19402881, 20451457, 18354305
    };

    @Override
    public List<CharSequence> enable(PortNumber client, PortNumber line, boolean enable) {
        DeviceId deviceId = handler().data().deviceId();
        log.info("Discovering ZTE device {}", deviceId);

        Port clientPort = handler().get(DeviceService.class).getPort(deviceId, client);
        if (clientPort == null) {
            log.warn("{} does not exist on {}", client, deviceId);
            return Collections.emptyList();
        }

        String clientName = clientPort.annotations().value(OC_NAME);
        if (Strings.isNullOrEmpty(clientName)) {
            log.warn("{} annotations not exist on {}@{}", OC_NAME, client, deviceId);
            return Collections.emptyList();
        }

        Port linePort = handler().get(DeviceService.class).getPort(deviceId, line);
        if (linePort == null) {
            log.warn("{} does not exist on {}", line, deviceId);
            return Collections.emptyList();
        }

        String lineName = linePort.annotations().value(OC_NAME);
        if (Strings.isNullOrEmpty(lineName)) {
            log.warn("{} annotations not exist on {}@{}", OC_NAME, line, deviceId);
            return Collections.emptyList();
        }

        int clientIndex, lineIndex;

        try {
            clientIndex = getPortIndex(clientName);
            lineIndex = getPortIndex(lineName);
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }

        // create <terminal-device xmlns="http://openconfig.net/yang/terminal-device">
        //        </terminal-device>
        OpenConfigTerminalDeviceHandler terminalDevice = new OpenConfigTerminalDeviceHandler();
        // add <logical-channels></logical-channels>
        OpenConfigLogicalChannelsHandler logicalChannels =
                new OpenConfigLogicalChannelsHandler(terminalDevice);
        // add <channel><index>"clientIndex"</index></channel>
        OpenConfigChannelHandler channel =
                new OpenConfigChannelHandler(clientIndex, logicalChannels);

        // add <channel xc:operation="merge/delete">
        if (enable) {
            channel.addAnnotation(ANOTATION_NAME, Operation.MERGE.value());
        } else {
            channel.addAnnotation(ANOTATION_NAME, Operation.DELETE.value());
        }

        // add <config><index>"clientIndex"</index></config>
        OpenConfigConfigOfChannelHandler configOfChannel =
                new OpenConfigConfigOfChannelHandler(channel);
        configOfChannel.addIndex(clientIndex);

        // add <logical-channel-assignments></logical-channel-assignments>
        OpenConfigLogicalChannelAssignmentsHandler logicalChannelAssignments =
                new OpenConfigLogicalChannelAssignmentsHandler(channel);

        // add <assignment><index>1</index></assignment>
        OpenConfigAssignmentHandler assignment =
                new OpenConfigAssignmentHandler(1, logicalChannelAssignments);

        // add <config><assignment-type>LOGICAL_CHANNEL</assignment-type>
        //             <logical-channel>"lineIndex"</logical-channel>
        //     </config>
        OpenConfigConfigOfAssignmentHandler configOfAssignment =
                new OpenConfigConfigOfAssignmentHandler(assignment);
        configOfAssignment.addAssignmentType(AssignmentTypeEnum.LOGICAL_CHANNEL);
        configOfAssignment.addLogicalChannel("" + lineIndex);

        return terminalDevice.getListCharSequence();
    }

    // index should be fixed
    private int getPortIndex(String portName) throws IllegalArgumentException {
        //PORT-1-4-C1
        String[] portInfos = portName.split("-");
        if (portInfos.length == 4) {
            throw new IllegalArgumentException("ZTE device port name illegal.");
        }

        int slotIndex = Integer.parseInt(portInfos[2]);
        int index = Integer.parseInt(portInfos[3].substring(1));

        if (portInfos[3].startsWith("C")) {
            return clientPortIndexs[index * 4 - slotIndex];
        } else if (portInfos[3].startsWith("C")) {
            return linePortIndexs[index * 4 - slotIndex];
        }

        throw new IllegalArgumentException("Connot match port index for ZTE device.");
    }

}
