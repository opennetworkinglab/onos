/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.drivers.lumentum;

import com.google.common.base.Preconditions;
import org.onlab.util.Frequency;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

import java.io.IOException;
import java.util.List;

/**
 * Quick and dirty device abstraction for SNMP-based Lumentum devices.
 * <p>
 * TODO: Refactor once SnmpDevice is finished
 */
public class LumentumSnmpDevice {

    private static final int MAX_SIZE_RESPONSE_PDU = 65535;
    private static final int MAX_REPETITIONS = 50;      // Only 42 directed ports on our devices

    public static final GridType GRID_TYPE = GridType.DWDM;
    public static final ChannelSpacing CHANNEL_SPACING = ChannelSpacing.CHL_50GHZ;
    public static final Frequency START_CENTER_FREQ = Frequency.ofGHz(191_350);
    public static final Frequency END_CENTER_FREQ = Frequency.ofGHz(196_100);

    // Lumentum SDN ROADM has shifted channel plan.
    // Channel 36 corresponds to ITU-T center frequency, which has spacing multiplier 0.
    public static final int MULTIPLIER_SHIFT = 36;

    private Snmp snmp;
    private CommunityTarget target;

    public LumentumSnmpDevice(DeviceId did) throws IOException {
        String[] deviceComponents = did.toString().split(":");
        Preconditions.checkArgument(deviceComponents.length > 1);

        String ipAddress = deviceComponents[1];
        String port = deviceComponents[2];

        createDevice(ipAddress, Integer.parseInt(port));
    }

    public LumentumSnmpDevice(String ipAddress, int port) throws IOException {
        createDevice(ipAddress, port);
    }

    private void createDevice(String ipAddress, int port) throws IOException {
        Address targetAddress = GenericAddress.parse("udp:" + ipAddress + "/" + port);
        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();
        snmp = new Snmp(transport);

        // setting up target
        target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        target.setAddress(targetAddress);
        target.setRetries(3);
        target.setTimeout(1000 * 3);
        target.setVersion(SnmpConstants.version2c);
        target.setMaxSizeRequestPDU(MAX_SIZE_RESPONSE_PDU);
    }

    public ResponseEvent set(PDU pdu) throws IOException {
        return snmp.set(pdu, target);
    }

    public List<TreeEvent> get(OID oid) {
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        treeUtils.setMaxRepetitions(MAX_REPETITIONS);
        return treeUtils.getSubtree(target, oid);
    }
}
