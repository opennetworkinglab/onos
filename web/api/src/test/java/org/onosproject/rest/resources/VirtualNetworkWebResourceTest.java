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

package org.onosproject.rest.resources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.glassfish.jersey.client.ClientProperties;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.incubator.net.virtual.DefaultVirtualDevice;
import org.onosproject.incubator.net.virtual.DefaultVirtualHost;
import org.onosproject.incubator.net.virtual.DefaultVirtualLink;
import org.onosproject.incubator.net.virtual.DefaultVirtualNetwork;
import org.onosproject.incubator.net.virtual.DefaultVirtualPort;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualHost;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Unit tests for virtual network REST APIs.
 */
public class VirtualNetworkWebResourceTest extends ResourceTest {

    private final VirtualNetworkAdminService mockVnetAdminService = createMock(VirtualNetworkAdminService.class);
    private final VirtualNetworkService mockVnetService = createMock(VirtualNetworkService.class);
    private CodecManager codecService;

    private final HashSet<VirtualDevice> vdevSet = new HashSet<>();
    private final HashSet<VirtualPort> vportSet = new HashSet<>();

    private static final String ID = "networkId";
    private static final String TENANT_ID = "tenantId";
    private static final String DEVICE_ID = "deviceId";
    private static final String PORT_NUM = "portNum";
    private static final String PHYS_DEVICE_ID = "physDeviceId";
    private static final String PHYS_PORT_NUM = "physPortNum";

    private final TenantId tenantId2 = TenantId.tenantId("TenantId2");
    private final TenantId tenantId3 = TenantId.tenantId("TenantId3");
    private final TenantId tenantId4 = TenantId.tenantId("TenantId4");

    private final NetworkId networkId1 = NetworkId.networkId(1);
    private final NetworkId networkId2 = NetworkId.networkId(2);
    private final NetworkId networkId3 = NetworkId.networkId(3);
    private final NetworkId networkId4 = NetworkId.networkId(4);

    private final VirtualNetwork vnet1 = new DefaultVirtualNetwork(networkId1, tenantId3);
    private final VirtualNetwork vnet2 = new DefaultVirtualNetwork(networkId2, tenantId3);
    private final VirtualNetwork vnet3 = new DefaultVirtualNetwork(networkId3, tenantId3);
    private final VirtualNetwork vnet4 = new DefaultVirtualNetwork(networkId4, tenantId3);

    private final DeviceId devId1 = DeviceId.deviceId("devid1");
    private final DeviceId devId2 = DeviceId.deviceId("devid2");
    private final DeviceId devId22 = DeviceId.deviceId("dev22");

    private final VirtualDevice vdev1 = new DefaultVirtualDevice(networkId3, devId1);
    private final VirtualDevice vdev2 = new DefaultVirtualDevice(networkId3, devId2);

    private final Device dev1 = NetTestTools.device("dev1");
    private final Device dev2 = NetTestTools.device("dev2");
    private final Device dev22 = NetTestTools.device("dev22");

    private final Port port1 = new DefaultPort(dev1, portNumber(1), true);
    private final Port port2 = new DefaultPort(dev2, portNumber(2), true);

    private final VirtualPort vport22 = new DefaultVirtualPort(networkId3,
                                                               dev22, portNumber(22), port1);
    private final VirtualPort vport23 = new DefaultVirtualPort(networkId3,
                                                               dev22, portNumber(23), port2);

    private final ConnectPoint cp11 = NetTestTools.connectPoint(devId1.toString(), 21);
    private final ConnectPoint cp21 = NetTestTools.connectPoint(devId2.toString(), 22);
    private final ConnectPoint cp12 = NetTestTools.connectPoint(devId1.toString(), 2);
    private final ConnectPoint cp22 = NetTestTools.connectPoint(devId2.toString(), 22);

    private final VirtualLink vlink1 = DefaultVirtualLink.builder()
            .networkId(networkId3)
            .src(cp22)
            .dst(cp11)
            .build();

    private final VirtualLink vlink2 = DefaultVirtualLink.builder()
            .networkId(networkId3)
            .src(cp12)
            .dst(cp21)
            .build();

    private final MacAddress mac1 = MacAddress.valueOf("00:11:00:00:00:01");
    private final MacAddress mac2 = MacAddress.valueOf("00:22:00:00:00:02");
    private final VlanId vlan1 = VlanId.vlanId((short) 11);
    private final VlanId vlan2 = VlanId.vlanId((short) 22);
    private final IpAddress ip1 = IpAddress.valueOf("10.0.0.1");
    private final IpAddress ip2 = IpAddress.valueOf("10.0.0.2");
    private final IpAddress ip3 = IpAddress.valueOf("10.0.0.3");

    private final HostId hId1 = HostId.hostId(mac1, vlan1);
    private final HostId hId2 = HostId.hostId(mac2, vlan2);
    private final HostLocation loc1 = new HostLocation(devId1, portNumber(100), 123L);
    private final HostLocation loc2 = new HostLocation(devId2, portNumber(200), 123L);
    private final Set<IpAddress> ipSet1 = Sets.newHashSet(ip1, ip2);
    private final Set<IpAddress> ipSet2 = Sets.newHashSet(ip1, ip3);
    private final VirtualHost vhost1 = new DefaultVirtualHost(networkId1, hId1,
                                                              mac1, vlan1, loc1, ipSet1);
    private final VirtualHost vhost2 = new DefaultVirtualHost(networkId2, hId2,
                                                              mac2, vlan2, loc2, ipSet2);




    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        // Register the services needed for the test
        codecService = new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(VirtualNetworkAdminService.class, mockVnetAdminService)
                        .add(VirtualNetworkService.class, mockVnetService)
                        .add(CodecService.class, codecService);

        BaseResource.setServiceDirectory(testDirectory);
    }

    /**
     * Hamcrest matcher to check that a virtual network entity representation in JSON matches
     * the actual virtual network entity.
     */
    private static final class JsonObjectMatcher<T> extends TypeSafeMatcher<JsonObject> {
        private final T vnetEntity;
        private List<String> jsonFieldNames;
        private String reason = "";
        private BiFunction<T, String, String> getValue; // get vnetEntity's value

        private JsonObjectMatcher(T vnetEntityValue,
                                  List<String> jsonFieldNames1,
                                  BiFunction<T, String, String> getValue1) {
            vnetEntity = vnetEntityValue;
            jsonFieldNames = jsonFieldNames1;
            getValue = getValue1;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonHost) {
            return jsonFieldNames
                    .stream()
                    .allMatch(s -> checkField(jsonHost, s, getValue.apply(vnetEntity, s)));
        }

        private boolean checkField(JsonObject jsonHost, String jsonFieldName,
                                   String objectValue) {
            final String jsonValue = jsonHost.get(jsonFieldName).asString();
            if (!jsonValue.equals(objectValue)) {
                reason = jsonFieldName + " " + objectValue;
                return false;
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate a virtual network id array matcher.
     *
     * @param obj virtual network id object we are looking for
     * @return matcher
     */
    /**
     * Factory to allocate a virtual network entity matcher.
     *
     * @param obj            virtual network object we are looking for
     * @param jsonFieldNames JSON field names to check against
     * @param getValue       function to retrieve value from virtual network object
     * @param <T>            the type of virtual network object
     * @return matcher
     */
    private static <T> JsonObjectMatcher matchesVnetEntity(T obj, List<String> jsonFieldNames,
                                                           BiFunction<T, String, String> getValue) {
        return new JsonObjectMatcher<T>(obj, jsonFieldNames, getValue);
    }

    /**
     * Hamcrest matcher to check that a virtual network entity is represented properly in a JSON
     * array of virtual network entities.
     */
    protected static class JsonArrayMatcher<T> extends TypeSafeMatcher<JsonArray> {
        private final T vnetEntity;
        private String reason = "";
        private Function<T, String> getKey; // gets vnetEntity's key
        private BiPredicate<T, JsonObject> checkKey; // check vnetEntity's key with JSON rep'n
        private List<String> jsonFieldNames; // field/property names
        private BiFunction<T, String, String> getValue; // get vnetEntity's value

        protected JsonArrayMatcher(T vnetEntityValue, Function<T, String> getKey1,
                                   BiPredicate<T, JsonObject> checkKey1,
                                   List<String> jsonFieldNames1,
                                   BiFunction<T, String, String> getValue1) {
            vnetEntity = vnetEntityValue;
            getKey = getKey1;
            checkKey = checkKey1;
            jsonFieldNames = jsonFieldNames1;
            getValue = getValue1;
        }

        @Override
        public boolean matchesSafely(JsonArray json) {
            boolean itemFound = false;
            final int expectedAttributes = jsonFieldNames.size();
            for (int jsonArrayIndex = 0; jsonArrayIndex < json.size();
                 jsonArrayIndex++) {

                final JsonObject jsonHost = json.get(jsonArrayIndex).asObject();

                if (jsonHost.names().size() < expectedAttributes) {
                    reason = "Found a virtual network with the wrong number of attributes";
                    return false;
                }

                if (checkKey != null && checkKey.test(vnetEntity, jsonHost)) {
                    itemFound = true;
                    assertThat(jsonHost, matchesVnetEntity(vnetEntity, jsonFieldNames, getValue));
                }
            }
            if (!itemFound) {
                reason = getKey.apply(vnetEntity) + " was not found";
                return false;
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Array matcher for VirtualNetwork.
     */
    private static final class VnetJsonArrayMatcher extends JsonArrayMatcher<VirtualNetwork> {

        private VnetJsonArrayMatcher(VirtualNetwork vnetIn) {
            super(vnetIn,
                  vnet -> "Virtual network " + vnet.id().toString(),
                  (vnet, jsonObject) -> jsonObject.get(ID).asString().equals(vnet.id().toString()),
                  ImmutableList.of(ID, TENANT_ID),
                  (vnet, s) -> s.equals(ID) ? vnet.id().toString()
                          : s.equals(TENANT_ID) ? vnet.tenantId().toString()
                          : null
            );
        }
    }

    /**
     * Factory to allocate a virtual network array matcher.
     *
     * @param vnet virtual network object we are looking for
     * @return matcher
     */
    private VnetJsonArrayMatcher hasVnet(VirtualNetwork vnet) {
        return new VnetJsonArrayMatcher(vnet);
    }

    // Tests for Virtual Networks

    /**
     * Tests the result of the REST API GET when there are no virtual networks.
     */
    @Test
    public void testGetVirtualNetworksEmptyArray() {
        expect(mockVnetAdminService.getTenantIds()).andReturn(ImmutableSet.of()).anyTimes();
        replay(mockVnetAdminService);
        expect(mockVnetService.getVirtualNetworks(tenantId4)).andReturn(ImmutableSet.of()).anyTimes();
        replay(mockVnetService);

        WebTarget wt = target();
        String response = wt.path("vnets").request().get(String.class);
        assertThat(response, is("{\"vnets\":[]}"));

        verify(mockVnetService);
        verify(mockVnetAdminService);
    }

    /**
     * Tests the result of the REST API GET when virtual networks are defined.
     */
    @Test
    public void testGetVirtualNetworksArray() {
        final Set<VirtualNetwork> vnetSet = ImmutableSet.of(vnet1, vnet2, vnet3, vnet4);
        expect(mockVnetAdminService.getTenantIds()).andReturn(ImmutableSet.of(tenantId3)).anyTimes();
        replay(mockVnetAdminService);
        expect(mockVnetService.getVirtualNetworks(tenantId3)).andReturn(vnetSet).anyTimes();
        replay(mockVnetService);

        WebTarget wt = target();
        String response = wt.path("vnets").request().get(String.class);
        assertThat(response, containsString("{\"vnets\":["));

        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("vnets"));

        final JsonArray vnetJsonArray = result.get("vnets").asArray();
        assertThat(vnetJsonArray, notNullValue());
        assertEquals("Virtual networks array is not the correct size.",
                     vnetSet.size(), vnetJsonArray.size());

        vnetSet.forEach(vnet -> assertThat(vnetJsonArray, hasVnet(vnet)));

        verify(mockVnetService);
        verify(mockVnetAdminService);
    }

    /**
     * Tests the result of the REST API GET for virtual networks with tenant id.
     */
    @Test
    public void testGetVirtualNetworksByTenantId() {
        final Set<VirtualNetwork> vnetSet = ImmutableSet.of(vnet1, vnet2, vnet3, vnet4);
        expect(mockVnetAdminService.getTenantIds()).andReturn(ImmutableSet.of(tenantId3)).anyTimes();
        replay(mockVnetAdminService);
        expect(mockVnetService.getVirtualNetworks(tenantId3)).andReturn(vnetSet).anyTimes();
        replay(mockVnetService);

        WebTarget wt = target();
        String response = wt.path("vnets/" + tenantId3.id()).request().get(String.class);
        assertThat(response, containsString("{\"vnets\":["));

        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("vnets"));

        final JsonArray vnetJsonArray = result.get("vnets").asArray();
        assertThat(vnetJsonArray, notNullValue());
        assertEquals("Virtual networks array is not the correct size.",
                     vnetSet.size(), vnetJsonArray.size());

        vnetSet.forEach(vnet -> assertThat(vnetJsonArray, hasVnet(vnet)));

        verify(mockVnetService);
        verify(mockVnetAdminService);
    }

    /**
     * Tests the result of the REST API GET for virtual networks with tenant id.
     */
    @Test
    public void testGetVirtualNetworksByNonExistentTenantId() {
        String tenantIdName = "NON_EXISTENT_TENANT_ID";
        expect(mockVnetAdminService.getTenantIds()).andReturn(ImmutableSet.of(tenantId3)).anyTimes();
        replay(mockVnetAdminService);
        expect(mockVnetService.getVirtualNetworks(anyObject())).andReturn(ImmutableSet.of()).anyTimes();
        replay(mockVnetService);

        WebTarget wt = target();

        try {
            wt.path("vnets/" + tenantIdName)
                    .request()
                    .get(String.class);
            fail("Get of a non-existent virtual network did not throw an exception");
        } catch (NotFoundException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 404 Not Found"));
        }

        verify(mockVnetService);
        verify(mockVnetAdminService);
    }

    /**
     * Tests adding of new virtual network using POST via JSON stream.
     */
    @Test
    public void testPostVirtualNetwork() {
        expect(mockVnetAdminService.createVirtualNetwork(tenantId2)).andReturn(vnet1);
        expectLastCall();

        replay(mockVnetAdminService);

        WebTarget wt = target();
        InputStream jsonStream = TenantWebResourceTest.class
                .getResourceAsStream("post-tenant.json");

        Response response = wt.path("vnets").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));

        String location = response.getLocation().getPath();
        assertThat(location, Matchers.startsWith("/vnets/" + vnet1.id().toString()));

        verify(mockVnetAdminService);
    }

    /**
     * Tests adding of a null virtual network using POST via JSON stream.
     */
    @Test
    public void testPostVirtualNetworkNullTenantId() {

        replay(mockVnetAdminService);

        WebTarget wt = target();
        try {
            wt.path("vnets")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.json(null), String.class);
            fail("POST of null virtual network did not throw an exception");
        } catch (BadRequestException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 400 Bad Request"));
        }

        verify(mockVnetAdminService);
    }

    /**
     * Tests removing a virtual network with DELETE request.
     */
    @Test
    public void testDeleteVirtualNetwork() {
        mockVnetAdminService.removeVirtualNetwork(anyObject());
        expectLastCall();
        replay(mockVnetAdminService);

        WebTarget wt = target()
                .property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        Response response = wt.path("vnets/" + "2")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();

        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NO_CONTENT));

        verify(mockVnetAdminService);
    }

    /**
     * Tests that a DELETE of a non-existent virtual network throws an exception.
     */
    @Test
    public void testDeleteNetworkNonExistentNetworkId() {
        expect(mockVnetAdminService.getTenantIds())
                .andReturn(ImmutableSet.of())
                .anyTimes();
        expectLastCall();

        replay(mockVnetAdminService);

        WebTarget wt = target();

        try {
            wt.path("vnets/" + "NON_EXISTENT_NETWORK_ID")
                    .request()
                    .delete(String.class);
            fail("Delete of a non-existent virtual network did not throw an exception");
        } catch (NotFoundException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 404 Not Found"));
        }

        verify(mockVnetAdminService);
    }

    // Tests for Virtual Device

    /**
     * Tests the result of the REST API GET when there are no virtual devices.
     */
    @Test
    public void testGetVirtualDevicesEmptyArray() {
        NetworkId networkId = networkId4;
        expect(mockVnetService.getVirtualDevices(networkId)).andReturn(ImmutableSet.of()).anyTimes();
        replay(mockVnetService);

        WebTarget wt = target();
        String location = "vnets/" + networkId.toString() + "/devices";
        String response = wt.path(location).request().get(String.class);
        assertThat(response, is("{\"devices\":[]}"));

        verify(mockVnetService);
    }

    /**
     * Tests the result of the REST API GET when virtual devices are defined.
     */
    @Test
    public void testGetVirtualDevicesArray() {
        NetworkId networkId = networkId3;
        vdevSet.add(vdev1);
        vdevSet.add(vdev2);
        expect(mockVnetService.getVirtualDevices(networkId)).andReturn(vdevSet).anyTimes();
        replay(mockVnetService);

        WebTarget wt = target();
        String location = "vnets/" + networkId.toString() + "/devices";
        String response = wt.path(location).request().get(String.class);
        assertThat(response, containsString("{\"devices\":["));

        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("devices"));

        final JsonArray vnetJsonArray = result.get("devices").asArray();
        assertThat(vnetJsonArray, notNullValue());
        assertEquals("Virtual devices array is not the correct size.",
                     vdevSet.size(), vnetJsonArray.size());

        vdevSet.forEach(vdev -> assertThat(vnetJsonArray, hasVdev(vdev)));

        verify(mockVnetService);
    }

    /**
     * Array matcher for VirtualDevice.
     */
    private static final class VdevJsonArrayMatcher extends JsonArrayMatcher<VirtualDevice> {

        private VdevJsonArrayMatcher(VirtualDevice vdevIn) {
            super(vdevIn,
                  vdev -> "Virtual device " + vdev.networkId().toString()
                          + " " + vdev.id().toString(),
                  (vdev, jsonObject) -> jsonObject.get(ID).asString().equals(vdev.networkId().toString())
                          && jsonObject.get(DEVICE_ID).asString().equals(vdev.id().toString()),
                  ImmutableList.of(ID, DEVICE_ID),
                  (vdev, s) -> s.equals(ID) ? vdev.networkId().toString()
                          : s.equals(DEVICE_ID) ? vdev.id().toString()
                          : null
            );
        }
    }

    /**
     * Factory to allocate a virtual device array matcher.
     *
     * @param vdev virtual device object we are looking for
     * @return matcher
     */
    private VdevJsonArrayMatcher hasVdev(VirtualDevice vdev) {
        return new VdevJsonArrayMatcher(vdev);
    }
    /**
     * Tests adding of new virtual device using POST via JSON stream.
     */
    @Test
    public void testPostVirtualDevice() {
        NetworkId networkId = networkId3;
        DeviceId deviceId = devId2;
        expect(mockVnetAdminService.createVirtualDevice(networkId, deviceId)).andReturn(vdev2);
        expectLastCall();

        replay(mockVnetAdminService);

        WebTarget wt = target();
        InputStream jsonStream = VirtualNetworkWebResourceTest.class
                .getResourceAsStream("post-virtual-device.json");
        String reqLocation = "vnets/" + networkId.toString() + "/devices";
        Response response = wt.path(reqLocation).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));

        String location = response.getLocation().getPath();
        assertThat(location, Matchers.startsWith("/" + reqLocation + "/" + vdev2.id().toString()));

        verify(mockVnetAdminService);
    }

    /**
     * Tests adding of a null virtual device using POST via JSON stream.
     */
    @Test
    public void testPostVirtualDeviceNullJsonStream() {
        NetworkId networkId = networkId3;
        replay(mockVnetAdminService);

        WebTarget wt = target();
        try {
            String reqLocation = "vnets/" + networkId.toString() + "/devices";
            wt.path(reqLocation)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.json(null), String.class);
            fail("POST of null virtual device did not throw an exception");
        } catch (BadRequestException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 400 Bad Request"));
        }

        verify(mockVnetAdminService);
    }

    /**
     * Tests removing a virtual device with DELETE request.
     */
    @Test
    public void testDeleteVirtualDevice() {
        NetworkId networkId = networkId3;
        DeviceId deviceId = devId2;
        mockVnetAdminService.removeVirtualDevice(networkId, deviceId);
        expectLastCall();
        replay(mockVnetAdminService);

        WebTarget wt = target()
                .property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        String reqLocation = "vnets/" + networkId.toString() + "/devices/" + deviceId.toString();
        Response response = wt.path(reqLocation)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();

        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NO_CONTENT));

        verify(mockVnetAdminService);
    }

    // Tests for Virtual Ports

    /**
     * Tests the result of the REST API GET when there are no virtual ports.
     */
    @Test
    public void testGetVirtualPortsEmptyArray() {
        NetworkId networkId = networkId4;
        DeviceId deviceId = devId2;
        expect(mockVnetService.getVirtualPorts(networkId, deviceId))
                .andReturn(ImmutableSet.of()).anyTimes();
        replay(mockVnetService);

        WebTarget wt = target();
        String location = "vnets/" + networkId.toString()
                + "/devices/" + deviceId.toString() + "/ports";
        String response = wt.path(location).request().get(String.class);
        assertThat(response, is("{\"ports\":[]}"));

        verify(mockVnetService);
    }

    /**
     * Tests the result of the REST API GET when virtual ports are defined.
     */
    @Test
    public void testGetVirtualPortsArray() {
        NetworkId networkId = networkId3;
        DeviceId deviceId = dev22.id();
        vportSet.add(vport23);
        vportSet.add(vport22);
        expect(mockVnetService.getVirtualPorts(networkId, deviceId)).andReturn(vportSet).anyTimes();
        replay(mockVnetService);

        WebTarget wt = target();
        String location = "vnets/" + networkId.toString()
                + "/devices/" + deviceId.toString() + "/ports";
        String response = wt.path(location).request().get(String.class);
        assertThat(response, containsString("{\"ports\":["));

        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("ports"));

        final JsonArray vnetJsonArray = result.get("ports").asArray();
        assertThat(vnetJsonArray, notNullValue());
        assertEquals("Virtual ports array is not the correct size.",
                     vportSet.size(), vnetJsonArray.size());

        vportSet.forEach(vport -> assertThat(vnetJsonArray, hasVport(vport)));

        verify(mockVnetService);
    }

    /**
     * Array matcher for VirtualPort.
     */
    private static final class VportJsonArrayMatcher extends JsonArrayMatcher<VirtualPort> {

        private VportJsonArrayMatcher(VirtualPort vportIn) {
            super(vportIn,
                  vport -> "Virtual port " + vport.networkId().toString() + " "
                    + vport.element().id().toString() + " " + vport.number().toString(),
                  (vport, jsonObject) -> jsonObject.get(ID).asString().equals(vport.networkId().toString())
                          && jsonObject.get(PORT_NUM).asString().equals(vport.number().toString())
                          && jsonObject.get(DEVICE_ID).asString().equals(vport.element().id().toString()),
                  ImmutableList.of(ID, DEVICE_ID, PORT_NUM, PHYS_DEVICE_ID, PHYS_PORT_NUM),
                  (vport, s) -> s.equals(ID) ? vport.networkId().toString()
                          : s.equals(DEVICE_ID) ? vport.element().id().toString()
                          : s.equals(PORT_NUM) ? vport.number().toString()
                          : s.equals(PHYS_DEVICE_ID) ? vport.realizedBy().element().id().toString()
                          : s.equals(PHYS_PORT_NUM) ? vport.realizedBy().number().toString()
                          : null
            );
        }
    }

    /**
     * Factory to allocate a virtual port array matcher.
     *
     * @param vport virtual port object we are looking for
     * @return matcher
     */
    private VportJsonArrayMatcher hasVport(VirtualPort vport) {
        return new VportJsonArrayMatcher(vport);
    }

    /**
     * Tests adding of new virtual port using POST via JSON stream.
     */
    @Test
    public void testPostVirtualPort() {
        NetworkId networkId = networkId3;
        DeviceId deviceId = devId22;
        DefaultAnnotations annotations = DefaultAnnotations.builder().build();
        Device physDevice = new DefaultDevice(null, DeviceId.deviceId("dev1"),
                                              null, null, null, null, null, null, annotations);
        Port port1 = new DefaultPort(physDevice, portNumber(1), true);
        expect(mockVnetAdminService.createVirtualPort(networkId, deviceId, portNumber(22), port1))
                .andReturn(vport22);

        replay(mockVnetAdminService);

        WebTarget wt = target();
        InputStream jsonStream = VirtualNetworkWebResourceTest.class
                .getResourceAsStream("post-virtual-port.json");
        String reqLocation = "vnets/" + networkId.toString()
                + "/devices/" + deviceId.toString() + "/ports";
        Response response = wt.path(reqLocation).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));

        verify(mockVnetAdminService);
    }

    /**
     * Tests adding of a null virtual port using POST via JSON stream.
     */
    @Test
    public void testPostVirtualPortNullJsonStream() {
        NetworkId networkId = networkId3;
        DeviceId deviceId = devId2;
        replay(mockVnetAdminService);

        WebTarget wt = target();
        try {
            String reqLocation = "vnets/" + networkId.toString()
                    + "/devices/" + deviceId.toString() + "/ports";
            wt.path(reqLocation)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.json(null), String.class);
            fail("POST of null virtual port did not throw an exception");
        } catch (BadRequestException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 400 Bad Request"));
        }

        verify(mockVnetAdminService);
    }

    /**
     * Tests removing a virtual port with DELETE request.
     */
    @Test
    public void testDeleteVirtualPort() {
        NetworkId networkId = networkId3;
        DeviceId deviceId = devId2;
        PortNumber portNum = portNumber(2);
        mockVnetAdminService.removeVirtualPort(networkId, deviceId, portNum);
        expectLastCall();
        replay(mockVnetAdminService);

        WebTarget wt = target()
                .property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        String reqLocation = "vnets/" + networkId.toString()
                + "/devices/" + deviceId.toString() + "/ports/" + portNum.toLong();
        Response response = wt.path(reqLocation)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();

        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NO_CONTENT));

        verify(mockVnetAdminService);
    }

    // Tests for Virtual Links

    /**
     * Tests the result of the REST API GET when there are no virtual links.
     */
    @Test
    public void testGetVirtualLinksEmptyArray() {
        NetworkId networkId = networkId4;
        expect(mockVnetService.getVirtualLinks(networkId)).andReturn(ImmutableSet.of()).anyTimes();
        replay(mockVnetService);

        WebTarget wt = target();
        String location = "vnets/" + networkId.toString() + "/links";
        String response = wt.path(location).request().get(String.class);
        assertThat(response, is("{\"links\":[]}"));

        verify(mockVnetService);
    }

    /**
     * Tests the result of the REST API GET when virtual links are defined.
     */
    @Test
    public void testGetVirtualLinksArray() {
        NetworkId networkId = networkId3;
        final Set<VirtualLink> vlinkSet = ImmutableSet.of(vlink1, vlink2);
        expect(mockVnetService.getVirtualLinks(networkId)).andReturn(vlinkSet).anyTimes();
        replay(mockVnetService);

        WebTarget wt = target();
        String location = "vnets/" + networkId.toString() + "/links";
        String response = wt.path(location).request().get(String.class);
        assertThat(response, containsString("{\"links\":["));

        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("links"));

        final JsonArray vnetJsonArray = result.get("links").asArray();
        assertThat(vnetJsonArray, notNullValue());
        assertEquals("Virtual links array is not the correct size.",
                     vlinkSet.size(), vnetJsonArray.size());

        vlinkSet.forEach(vlink -> assertThat(vnetJsonArray, hasVlink(vlink)));

        verify(mockVnetService);
    }

    /**
     * Hamcrest matcher to check that a virtual link representation in JSON matches
     * the actual virtual link.
     */
    private static final class VirtualLinkJsonMatcher extends LinksResourceTest.LinkJsonMatcher {
        private final VirtualLink vlink;
        private String reason = "";

        private VirtualLinkJsonMatcher(VirtualLink vlinkValue) {
            super(vlinkValue);
            vlink = vlinkValue;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonLink) {
            if (!super.matchesSafely(jsonLink)) {
                return false;
            }
            // check NetworkId
            String jsonNetworkId = jsonLink.get(ID).asString();
            String networkId = vlink.networkId().toString();
            if (!jsonNetworkId.equals(networkId)) {
                reason = ID + " was " + jsonNetworkId;
                return false;
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate a virtual link matcher.
     *
     * @param vlink virtual link object we are looking for
     * @return matcher
     */
    private static VirtualLinkJsonMatcher matchesVirtualLink(VirtualLink vlink) {
        return new VirtualLinkJsonMatcher(vlink);
    }

    /**
     * Hamcrest matcher to check that a virtual link is represented properly in a JSON
     * array of links.
     */
    private static final class VirtualLinkJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final VirtualLink vlink;
        private String reason = "";

        private VirtualLinkJsonArrayMatcher(VirtualLink vlinkValue) {
            vlink = vlinkValue;
        }

        @Override
        public boolean matchesSafely(JsonArray json) {
            for (int jsonLinkIndex = 0; jsonLinkIndex < json.size();
                 jsonLinkIndex++) {

                JsonObject jsonLink = json.get(jsonLinkIndex).asObject();

                if (matchesVirtualLink(vlink).matchesSafely(jsonLink)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate a virtual link array matcher.
     *
     * @param vlink virtual link object we are looking for
     * @return matcher
     */
    private VirtualLinkJsonArrayMatcher hasVlink(VirtualLink vlink) {
        return new VirtualLinkJsonArrayMatcher(vlink);
    }

    /**
     * Tests adding of new virtual link using POST via JSON stream.
     */
    @Test
    public void testPostVirtualLink() {
        NetworkId networkId = networkId3;
        expect(mockVnetAdminService.createVirtualLink(networkId, cp22, cp11))
                .andReturn(vlink1);
        replay(mockVnetAdminService);

        WebTarget wt = target();
        InputStream jsonStream = VirtualNetworkWebResourceTest.class
                .getResourceAsStream("post-virtual-link.json");
        String reqLocation = "vnets/" + networkId.toString() + "/links";
        Response response = wt.path(reqLocation).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));

        String location = response.getLocation().getPath();
        assertThat(location, Matchers.startsWith("/" + reqLocation));

        verify(mockVnetAdminService);
    }

    /**
     * Tests adding of a null virtual link using POST via JSON stream.
     */
    @Test
    public void testPostVirtualLinkNullJsonStream() {
        NetworkId networkId = networkId3;
        replay(mockVnetAdminService);

        WebTarget wt = target();
        try {
            String reqLocation = "vnets/" + networkId.toString() + "/links";
            wt.path(reqLocation)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.json(null), String.class);
            fail("POST of null virtual link did not throw an exception");
        } catch (BadRequestException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 400 Bad Request"));
        }

        verify(mockVnetAdminService);
    }

    /**
     * Tests removing a virtual link with DELETE request.
     */
    @Test
    public void testDeleteVirtualLink() {
        NetworkId networkId = networkId3;
        mockVnetAdminService.removeVirtualLink(networkId, cp22, cp11);
        expectLastCall();
        replay(mockVnetAdminService);

        WebTarget wt = target()
                .property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        InputStream jsonStream = VirtualNetworkWebResourceTest.class
                .getResourceAsStream("post-virtual-link.json");
        String reqLocation = "vnets/" + networkId.toString() + "/links";
        Response response = wt.path(reqLocation).request().method("DELETE", Entity.json(jsonStream));

        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NO_CONTENT));
        verify(mockVnetAdminService);
    }

    // Tests for Virtual Hosts

    /**
     * Tests the result of the REST API GET when there are no virtual hosts.
     */
    @Test
    public void testGetVirtualHostsEmptyArray() {
        NetworkId networkId = networkId4;
        expect(mockVnetService.getVirtualHosts(networkId)).andReturn(ImmutableSet.of()).anyTimes();
        replay(mockVnetService);

        WebTarget wt = target();
        String location = "vnets/" + networkId.toString() + "/hosts";
        String response = wt.path(location).request().get(String.class);
        assertThat(response, is("{\"hosts\":[]}"));

        verify(mockVnetService);
    }

    /**
     * Tests the result of the REST API GET when virtual hosts are defined.
     */
    @Test
    public void testGetVirtualHostsArray() {
        NetworkId networkId = networkId3;
        final Set<VirtualHost> vhostSet = ImmutableSet.of(vhost1, vhost2);
        expect(mockVnetService.getVirtualHosts(networkId)).andReturn(vhostSet).anyTimes();
        replay(mockVnetService);

        WebTarget wt = target();
        String location = "vnets/" + networkId.toString() + "/hosts";
        String response = wt.path(location).request().get(String.class);
        assertThat(response, containsString("{\"hosts\":["));

        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("hosts"));

        final JsonArray vnetJsonArray = result.get("hosts").asArray();
        assertThat(vnetJsonArray, notNullValue());
        assertEquals("Virtual hosts array is not the correct size.",
                     vhostSet.size(), vnetJsonArray.size());

        vhostSet.forEach(vhost -> assertThat(vnetJsonArray, hasVhost(vhost)));

        verify(mockVnetService);
    }

    /**
     * Hamcrest matcher to check that a virtual host representation in JSON matches
     * the actual virtual host.
     */
    private static final class VirtualHostJsonMatcher extends HostResourceTest.HostJsonMatcher {
        private final VirtualHost vhost;
        private String reason = "";

        private VirtualHostJsonMatcher(VirtualHost vhostValue) {
            super(vhostValue);
            vhost = vhostValue;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonHost) {
            if (!super.matchesSafely(jsonHost)) {
                return false;
            }
            // check NetworkId
            String jsonNetworkId = jsonHost.get(ID).asString();
            String networkId = vhost.networkId().toString();
            if (!jsonNetworkId.equals(networkId)) {
                reason = ID + " was " + jsonNetworkId;
                return false;
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate a virtual host matcher.
     *
     * @param vhost virtual host object we are looking for
     * @return matcher
     */
    private static VirtualHostJsonMatcher matchesVirtualHost(VirtualHost vhost) {
        return new VirtualHostJsonMatcher(vhost);
    }

    /**
     * Hamcrest matcher to check that a virtual host is represented properly in a JSON
     * array of hosts.
     */
    private static final class VirtualHostJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final VirtualHost vhost;
        private String reason = "";

        private VirtualHostJsonArrayMatcher(VirtualHost vhostValue) {
            vhost = vhostValue;
        }

        @Override
        public boolean matchesSafely(JsonArray json) {
            for (int jsonHostIndex = 0; jsonHostIndex < json.size();
                 jsonHostIndex++) {

                JsonObject jsonHost = json.get(jsonHostIndex).asObject();

                if (matchesVirtualHost(vhost).matchesSafely(jsonHost)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate a virtual host array matcher.
     *
     * @param vhost virtual host object we are looking for
     * @return matcher
     */
    private VirtualHostJsonArrayMatcher hasVhost(VirtualHost vhost) {
        return new VirtualHostJsonArrayMatcher(vhost);
    }

    /**
     * Tests adding of new virtual host using POST via JSON stream.
     */
    @Test
    public void testPostVirtualHost() {
        NetworkId networkId = networkId3;
        expect(mockVnetAdminService.createVirtualHost(networkId, hId1, mac1, vlan1, loc1, ipSet1))
                .andReturn(vhost1);
        replay(mockVnetAdminService);

        WebTarget wt = target();
        InputStream jsonStream = VirtualNetworkWebResourceTest.class
                .getResourceAsStream("post-virtual-host.json");
        String reqLocation = "vnets/" + networkId.toString() + "/hosts";
        Response response = wt.path(reqLocation).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));

        String location = response.getLocation().getPath();
        assertThat(location, Matchers.startsWith("/" + reqLocation));

        verify(mockVnetAdminService);
    }

    /**
     * Tests adding of a null virtual host using POST via JSON stream.
     */
    @Test
    public void testPostVirtualHostNullJsonStream() {
        NetworkId networkId = networkId3;
        replay(mockVnetAdminService);

        WebTarget wt = target();
        try {
            String reqLocation = "vnets/" + networkId.toString() + "/hosts";
            wt.path(reqLocation)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.json(null), String.class);
            fail("POST of null virtual host did not throw an exception");
        } catch (BadRequestException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 400 Bad Request"));
        }

        verify(mockVnetAdminService);
    }

    /**
     * Tests removing a virtual host with DELETE request.
     */
    @Test
    public void testDeleteVirtualHost() {
        NetworkId networkId = networkId3;
        mockVnetAdminService.removeVirtualHost(networkId, hId1);
        expectLastCall();
        replay(mockVnetAdminService);

        WebTarget wt = target()
                .property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        InputStream jsonStream = VirtualNetworkWebResourceTest.class
                .getResourceAsStream("post-virtual-host.json");
        String reqLocation = "vnets/" + networkId.toString() + "/hosts";
        Response response = wt.path(reqLocation).request().method("DELETE", Entity.json(jsonStream));

        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NO_CONTENT));
        verify(mockVnetAdminService);
    }
}
