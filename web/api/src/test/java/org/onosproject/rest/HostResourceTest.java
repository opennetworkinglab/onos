/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.rest;


import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Set;

import com.sun.jersey.api.client.ClientResponse;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableSet;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.onlab.packet.MacAddress.valueOf;
import static org.onlab.packet.VlanId.vlanId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Simple example on how to write a JAX-RS unit test using Jersey test framework.
 * A base class should/will be created to provide further assistance for testing.
 */
public class HostResourceTest extends ResourceTest {
    final HostService mockHostService = createMock(HostService.class);
    final HostProviderRegistry mockHostProviderRegistry = createMock(HostProviderRegistry.class);
    final HostProviderService mockHostProviderService = createMock(HostProviderService.class);
    final HashSet<Host> hosts = new HashSet<>();

    /**
     * Initializes test mocks and environment.
     */
    @Before
    public void setUpTest() {
        expect(mockHostService.getHosts()).andReturn(hosts).anyTimes();

        // Register the services needed for the test
        final CodecManager codecService =  new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(HostService.class, mockHostService)
                        .add(CodecService.class, codecService)
                        .add(HostProviderRegistry.class, mockHostProviderRegistry);
        BaseResource.setServiceDirectory(testDirectory);
    }

    /**
     * Verifies mocks.
     */
    @After
    public void tearDownTest() {
        verify(mockHostService);
    }

    /**
     * Hamcrest matcher to check that a host representation in JSON matches
     * the actual host.
     */
    public static class HostJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final Host host;
        private String reason = "";

        public HostJsonMatcher(Host hostValue) {
            host = hostValue;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonHost) {
            // Check id
            final String jsonId = jsonHost.get("id").asString();
            if (!jsonId.equals(host.id().toString())) {
                reason = "id " + host.id().toString();
                return false;
            }

            // Check vlan id
            final String jsonVlanId = jsonHost.get("vlan").asString();
            if (!jsonVlanId.equals(host.vlan().toString())) {
                reason = "vlan id " + host.vlan().toString();
                return false;
            }

            // Check mac address
            final String jsonMacAddress = jsonHost.get("mac").asString();
            if (!jsonMacAddress.equals(host.mac().toString())) {
                reason = "mac address " + host.mac().toString();
                return false;
            }

            // Check location element id
            final JsonObject jsonLocation = jsonHost.get("location").asObject();
            final String jsonLocationElementId = jsonLocation.get("elementId").asString();
            if (!jsonLocationElementId.equals(host.location().elementId().toString())) {
                reason = "location element id " + host.location().elementId().toString();
                return false;
            }

            // Check location port number
            final String jsonLocationPortNumber = jsonLocation.get("port").asString();
            if (!jsonLocationPortNumber.equals(host.location().port().toString())) {
                reason = "location portNumber " + host.location().port().toString();
                return false;
            }

            //  Check Ip Addresses
            final JsonArray jsonHostIps = jsonHost.get("ipAddresses").asArray();
            final Set<IpAddress> expectedHostIps = host.ipAddresses();
            if (jsonHostIps.size() != expectedHostIps.size()) {
                reason = "IP address arrays differ in size";
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
     * Factory to allocate a host array matcher.
     *
     * @param host host object we are looking for
     * @return matcher
     */
    private static HostJsonMatcher matchesHost(Host host) {
        return new HostJsonMatcher(host);
    }

    /**
     * Hamcrest matcher to check that a host is represented properly in a JSON
     * array of hosts.
     */
    public static class HostJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final Host host;
        private String reason = "";

        public HostJsonArrayMatcher(Host hostValue) {
            host = hostValue;
        }

        @Override
        public boolean matchesSafely(JsonArray json) {
            boolean hostFound = false;
            final int expectedAttributes = 5;
            for (int jsonHostIndex = 0; jsonHostIndex < json.size();
                 jsonHostIndex++) {

                final JsonObject jsonHost = json.get(jsonHostIndex).asObject();

                if (jsonHost.names().size() != expectedAttributes) {
                    reason = "Found a host with the wrong number of attributes";
                    return false;
                }

                final String jsonHostId = jsonHost.get("id").asString();
                if (jsonHostId.equals(host.id().toString())) {
                    hostFound = true;

                    //  We found the correct host, check attribute values
                    assertThat(jsonHost, matchesHost(host));
                }
            }
            if (!hostFound) {
                reason = "Host with id " + host.id().toString() + " not found";
                return false;
            } else {
                return true;
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate a host array matcher.
     *
     * @param host host object we are looking for
     * @return matcher
     */
    private static HostJsonArrayMatcher hasHost(Host host) {
        return new HostJsonArrayMatcher(host);
    }

    /**
     * Tests the result of the rest api GET when there are no hosts.
     */
    @Test
    public void testHostsEmptyArray() {
        replay(mockHostService);
        WebResource rs = resource();
        String response = rs.path("hosts").get(String.class);
        assertThat(response, is("{\"hosts\":[]}"));
    }

    /**
     * Tests the result of the rest api GET when hosts are defined.
     */
    @Test
    public void testHostsArray() {
        replay(mockHostService);
        final ProviderId pid = new ProviderId("of", "foo");
        final MacAddress mac1 = MacAddress.valueOf("00:00:11:00:00:01");
        final Set<IpAddress> ips1 = ImmutableSet.of(IpAddress.valueOf("1111:1111:1111:1::"));
        final Host host1 =
                new DefaultHost(pid, HostId.hostId(mac1), valueOf(1), vlanId((short) 1),
                        new HostLocation(DeviceId.deviceId("1"), portNumber(11), 1),
                        ips1);
        final MacAddress mac2 = MacAddress.valueOf("00:00:11:00:00:02");
        final Set<IpAddress> ips2 = ImmutableSet.of(
                IpAddress.valueOf("2222:2222:2222:1::"),
                IpAddress.valueOf("2222:2222:2222:2::"));
        final Host host2 =
                new DefaultHost(pid, HostId.hostId(mac2), valueOf(2), vlanId((short) 2),
                        new HostLocation(DeviceId.deviceId("2"), portNumber(22), 2),
                        ips2);
        hosts.add(host1);
        hosts.add(host2);
        WebResource rs = resource();
        String response = rs.path("hosts").get(String.class);
        assertThat(response, containsString("{\"hosts\":["));

        final JsonObject result = JsonObject.readFrom(response);
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("hosts"));

        final JsonArray hosts = result.get("hosts").asArray();
        assertThat(hosts, notNullValue());

        assertThat(hosts, hasHost(host1));
        assertThat(hosts, hasHost(host2));
    }

    /**
     * Tests fetch of one host by Id.
     */
    @Test
    public void testSingleHostByIdFetch() {
        final ProviderId pid = new ProviderId("of", "foo");
        final MacAddress mac1 = MacAddress.valueOf("00:00:11:00:00:01");
        final Set<IpAddress> ips1 = ImmutableSet.of(IpAddress.valueOf("1111:1111:1111:1::"));
        final Host host1 =
                new DefaultHost(pid, HostId.hostId(mac1), valueOf(1), vlanId((short) 1),
                        new HostLocation(DeviceId.deviceId("1"), portNumber(11), 1),
                        ips1);

        hosts.add(host1);

        expect(mockHostService.getHost(HostId.hostId("00:00:11:00:00:01/1")))
                .andReturn(host1)
                .anyTimes();
        replay(mockHostService);

        WebResource rs = resource();
        String response = rs.path("hosts/00:00:11:00:00:01%2F1").get(String.class);
        final JsonObject result = JsonObject.readFrom(response);
        assertThat(result, matchesHost(host1));
    }

    /**
     * Tests fetch of one host by mac and vlan.
     */
    @Test
    public void testSingleHostByMacAndVlanFetch() {
        final ProviderId pid = new ProviderId("of", "foo");
        final MacAddress mac1 = MacAddress.valueOf("00:00:11:00:00:01");
        final Set<IpAddress> ips1 = ImmutableSet.of(IpAddress.valueOf("1111:1111:1111:1::"));
        final Host host1 =
                new DefaultHost(pid, HostId.hostId(mac1), valueOf(1), vlanId((short) 1),
                        new HostLocation(DeviceId.deviceId("1"), portNumber(11), 1),
                        ips1);

        hosts.add(host1);

        expect(mockHostService.getHost(HostId.hostId("00:00:11:00:00:01/1")))
                .andReturn(host1)
                .anyTimes();
        replay(mockHostService);

        WebResource rs = resource();
        String response = rs.path("hosts/00:00:11:00:00:01/1").get(String.class);
        final JsonObject result = JsonObject.readFrom(response);
        assertThat(result, matchesHost(host1));
    }

    /**
     * Tests that a fetch of a non-existent object throws an exception.
     */
    @Test
    public void testBadGet() {

            expect(mockHostService.getHost(HostId.hostId("00:00:11:00:00:01/1")))
                    .andReturn(null)
                    .anyTimes();
            replay(mockHostService);

            WebResource rs = resource();
        try {
            rs.path("hosts/00:00:11:00:00:01/1").get(String.class);
            fail("Fetch of non-existent host did not throw an exception");
        } catch (UniformInterfaceException ex) {
            assertThat(ex.getMessage(),
                    containsString("returned a response status of"));
        }
    }

    /**
     * Tests post of a single host via JSON stream.
     */
    @Test
    public void testPost() {
        mockHostProviderService.hostDetected(anyObject(), anyObject());
        expectLastCall();
        replay(mockHostProviderService);

        expect(mockHostProviderRegistry.register(anyObject())).andReturn(mockHostProviderService);
        mockHostProviderRegistry.unregister(anyObject());
        expectLastCall();
        replay(mockHostProviderRegistry);

        replay(mockHostService);

        InputStream jsonStream = IntentsResourceTest.class
                .getResourceAsStream("post-host.json");
        WebResource rs = resource();

        ClientResponse response = rs.path("hosts")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, jsonStream);
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));
        String location = response.getLocation().getPath();
        assertThat(location, Matchers.startsWith("/hosts/11:22:33:44:55:66/-1"));
    }
}

