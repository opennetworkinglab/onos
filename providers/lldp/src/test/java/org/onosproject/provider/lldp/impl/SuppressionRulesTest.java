/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.provider.lldp.impl;

import static org.junit.Assert.*;
import static org.onosproject.net.DeviceId.deviceId;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class SuppressionRulesTest {

    private static final DeviceId NON_SUPPRESSED_DID = deviceId("of:1111000000000000");
    private static final DeviceId SUPPRESSED_DID = deviceId("of:2222000000000000");
    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final String MFR = "whitebox";
    private static final String HW = "1.1.x";
    private static final String SW1 = "3.8.1";
    private static final String SN = "43311-12345";
    private static final ChassisId CID = new ChassisId();

    private static final PortNumber P1 = PortNumber.portNumber(1);

    private SuppressionRules rules;

    @Before
    public void setUp() throws Exception {
        rules = new SuppressionRules(ImmutableSet.of(Device.Type.ROADM, Device.Type.OTN),
                               ImmutableMap.of("no-lldp", SuppressionRules.ANY_VALUE,
                                               "sendLLDP", "false"));
    }

    @Test
    public void testSuppressedDeviceType() {
        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.ROADM,
                                          MFR, HW, SW1, SN, CID);
        assertTrue(rules.isSuppressed(device));
    }

    @Test
    public void testSuppressedDeviceAnnotation() {
        Annotations annotation = DefaultAnnotations.builder()
                .set("no-lldp", "random")
                .build();

        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID, annotation);
        assertTrue(rules.isSuppressed(device));
    }

    @Test
    public void testSuppressedDeviceAnnotationExact() {
        Annotations annotation = DefaultAnnotations.builder()
                .set("sendLLDP", "false")
                .build();

        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID, annotation);
        assertTrue(rules.isSuppressed(device));
    }

    @Test
    public void testNotSuppressedDevice() {
        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID);
        assertFalse(rules.isSuppressed(device));
    }

    @Test
    public void testSuppressedPortAnnotation() {
        Annotations annotation = DefaultAnnotations.builder()
                .set("no-lldp", "random")
                .build();
        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID);
        Port port = new DefaultPort(device, P1, true, annotation);

        assertTrue(rules.isSuppressed(port));
    }

    @Test
    public void testSuppressedPortAnnotationExact() {
        Annotations annotation = DefaultAnnotations.builder()
                .set("sendLLDP", "false")
                .build();
        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID);
        Port port = new DefaultPort(device, P1, true, annotation);

        assertTrue(rules.isSuppressed(port));
    }

    @Test
    public void testNotSuppressedPort() {
        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID);
        Port port = new DefaultPort(device, P1, true);

        assertFalse(rules.isSuppressed(port));
    }
}
