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
package org.onosproject.odtn.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Path;
import org.onosproject.net.link.LinkServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.TestStorageService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;

/**
 * Test for parsing gNPY json files.
 */
public class GnpyManagerTest {

    private static final String REQUEST = "{\"path-request\":[{\"request-id\":\"onos-0\"," +
            "\"source\":\"netconf:10.0.254.93:830\"," +
            "\"destination\":\"netconf:10.0.254.94:830\",\"src-tp-id\":" +
            "\"netconf:10.0.254.93:830\",\"dst-tp-id\":" +
            "\"netconf:10.0.254.94:830\",\"bidirectional\":true," +
            "\"path-constraints\":{\"te-bandwidth\":" +
            "{\"technology\":\"flexi-grid\",\"trx_type\":\"Cassini\"," +
            "\"trx_mode\":null,\"effective-freq-slot\":" +
            "[{\"N\":\"null\",\"M\":\"null\"}],\"spacing\":5.0E10," +
            "\"max-nb-of-channel\":null,\"output-power\":null," +
            "\"path_bandwidth\":1.0E11}}}]}";

    private ConnectPoint tx1 = ConnectPoint.fromString("netconf:10.0.254.93:830/1");
    private ConnectPoint rdm1tx1 = ConnectPoint.fromString("netconf:10.0.254.107:830/1");
    private ConnectPoint rdm1ln1 = ConnectPoint.fromString("netconf:10.0.254.107:830/2");
    private ConnectPoint ln1rdm1 = ConnectPoint.fromString("netconf:10.0.254.101:830/1");
    private ConnectPoint ln1ln2 = ConnectPoint.fromString("netconf:10.0.254.101:830/2");
    private ConnectPoint ln2ln1 = ConnectPoint.fromString("netconf:10.0.254.102:830/1");
    private ConnectPoint ln2rdm2 = ConnectPoint.fromString("netconf:10.0.254.102:830/2");
    private ConnectPoint rdm2ln2 = ConnectPoint.fromString("netconf:10.0.254.225:830/1");
    private ConnectPoint rdm2tx2 = ConnectPoint.fromString("netconf:10.0.254.225:830/2");
    private ConnectPoint tx2 = ConnectPoint.fromString("netconf:10.0.254.94:830/1");
    private Link tx1rdm1Link = DefaultLink.builder().type(Link.Type.OPTICAL)
            .providerId(ProviderId.NONE).src(rdm1tx1).dst(tx1).build();
    private Link rmd1ln1Link = DefaultLink.builder().type(Link.Type.OPTICAL)
            .providerId(ProviderId.NONE).src(ln1rdm1).dst(rdm1ln1).build();
    private Link ln1ln2Link = DefaultLink.builder().type(Link.Type.OPTICAL)
            .providerId(ProviderId.NONE).src(ln2ln1).dst(ln1ln2).build();
    private Link ln2rdm2Link = DefaultLink.builder().type(Link.Type.OPTICAL)
            .providerId(ProviderId.NONE).src(rdm2ln2).dst(ln2rdm2).build();
    private Link tx2rmd2Link = DefaultLink.builder().type(Link.Type.OPTICAL)
            .providerId(ProviderId.NONE).src(tx2).dst(rdm2tx2).build();

    private GnpyManager manager;
    private JsonNode reply;

    @Before
    public void setUp() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        reply = mapper.readTree(this.getClass().getResourceAsStream("gnpy-response.json"));
        manager = new GnpyManager();
        manager.storageService = new TestStorageService();
        manager.linkService = new InternalLinkService();
        manager.coreService = new InternalCoreService();
        manager.activate();
    }

    @Test
    public void testCreateSuggestedPath() throws IOException {
        Map<DeviceId, Double> deviceAtoBPowerMap = new HashMap<>();
        Map<DeviceId, Double> deviceBtoAPowerMap = new HashMap<>();
        List<DeviceId> deviceIds = manager.getDeviceAndPopulatePowerMap(reply, deviceAtoBPowerMap,
                                                                        deviceBtoAPowerMap, "second");
        Path path = manager.createSuggestedPath(deviceIds);
        assertTrue(path.links().contains(tx1rdm1Link));
        assertTrue(path.links().contains(rmd1ln1Link));
        assertTrue(path.links().contains(ln1ln2Link));
        assertTrue(path.links().contains(ln2rdm2Link));
        assertTrue(path.links().contains(tx2rmd2Link));
        assertEquals(path.src(), tx2);
        assertEquals(path.dst(), tx1);

    }

    @Test
    public void testgetDevicePowerMap() throws IOException {
        Map<DeviceId, Double> deviceAtoBPowerMap = new HashMap<>();
        Map<DeviceId, Double> deviceBtoAPowerMap = new HashMap<>();
        manager.getDeviceAndPopulatePowerMap(reply, deviceAtoBPowerMap, deviceBtoAPowerMap, "second");
        assertEquals(-25.0, deviceAtoBPowerMap.get(DeviceId.deviceId("netconf:10.0.254.107:830")));
        assertEquals(-12.0, deviceAtoBPowerMap.get(DeviceId.deviceId("netconf:10.0.254.225:830")));
        assertEquals(-12.0, deviceBtoAPowerMap.get(DeviceId.deviceId("netconf:10.0.254.225:830")));
        assertEquals(-25.0, deviceBtoAPowerMap.get(DeviceId.deviceId("netconf:10.0.254.107:830")));
    }

    @Test
    public void testGetLaunchPower() throws IOException {
        double power = manager.getLaunchPower(reply);
        assertEquals(0.0, power);
    }

    @Test
    public void testGetPerHopPower() throws IOException {
        JsonNode response = reply.get("result").get("response");
        //getting the a-b path.
        JsonNode responseObj = response.elements()
                .next();
        Iterator<JsonNode> elements = responseObj.get("path-properties")
                .get("path-route-objects").elements();
        Iterable<JsonNode> iterable = () -> elements;
        List<JsonNode> elementsList = StreamSupport
                .stream(iterable.spliterator(), false)
                .collect(Collectors.toList());
        double power = manager.getPerHopPower(elementsList.get(5));
        assertEquals(-12.0, power);
    }

    @Test
    public void testGetOsnr() throws IOException {
        double osnr = manager.getOsnr(reply, "second");
        assertEquals(23.47, osnr);
    }

    @Test
    public void testCreateOchSignal() throws IOException {
        OchSignal signal = manager.createOchSignal(reply);
        System.out.println(signal);
        assertEquals(signal.gridType(), GridType.DWDM);
        assertEquals(signal.slotWidth().asGHz(), 50.000);
        assertEquals(-35, signal.spacingMultiplier());
    }

    @Test
    public void testCreateGnpyRequest() {
        ConnectPoint ingress = ConnectPoint.fromString("netconf:10.0.254.93:830/1");
        ConnectPoint egress = ConnectPoint.fromString("netconf:10.0.254.94:830/1");
        String output = manager.createGnpyRequest(ingress, egress, true).toString();
        System.out.println(output);
        assertEquals("Json to create network connectivity is wrong", REQUEST, output);
    }

    private class InternalLinkService extends LinkServiceAdapter {
        @Override
        public Set<Link> getDeviceLinks(DeviceId deviceId) {
            if (deviceId.equals(DeviceId.deviceId("netconf:10.0.254.94:830"))) {
                return ImmutableSet.of(tx2rmd2Link);
            } else if (deviceId.equals(DeviceId.deviceId("netconf:10.0.254.107:830"))) {
                return ImmutableSet.of(tx1rdm1Link);
            } else if (deviceId.equals(DeviceId.deviceId("netconf:10.0.254.101:830"))) {
                return ImmutableSet.of(rmd1ln1Link);
            } else if (deviceId.equals(DeviceId.deviceId("netconf:10.0.254.102:830"))) {
                return ImmutableSet.of(ln1ln2Link);
            } else if (deviceId.equals(DeviceId.deviceId("netconf:10.0.254.225:830"))) {
                return ImmutableSet.of(ln2rdm2Link);
            }
            return ImmutableSet.of();
        }
    }

    private class InternalCoreService extends CoreServiceAdapter {
        @Override
        public ApplicationId getAppId(String name) {
            return new DefaultApplicationId(1, name);
        }
    }
//    private class InternalStoreService extends StorageServiceAdapter {
//        @Override
//        public AtomicCounterBuilder atomicCounterBuilder() {
//            return TestAtomicCounter.builder();
//        }
//    }
}
