/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.xosintegration;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * XOS interface application.
 */
@Component(immediate = true)
@Service
public class OnosXosIntegrationManager implements VoltTenantService {
    private static final String XOS_SERVER_ADDRESS_PROPERTY_NAME =
            "xosServerAddress";
    private static final String XOS_SERVER_PORT_PROPERTY_NAME =
            "xosServerPort";
    private static final String XOS_PROVIDER_SERVICE_PROPERTY_NAME =
            "xosProviderService";

    private static final String TEST_XOS_SERVER_ADDRESS = "10.254.1.22";
    private static final int TEST_XOS_SERVER_PORT = 8000;
    private static final String XOS_TENANT_BASE_URI = "/xoslib/volttenant/";
    private static final int TEST_XOS_PROVIDER_SERVICE = 1;

    private static final int PRIORITY = 50000;
    private static final DeviceId FABRIC_DEVICE_ID = DeviceId.deviceId("of:5e3e486e73000187");
    private static final PortNumber FABRIC_OLT_CONNECT_POINT = PortNumber.portNumber(2);
    private static final PortNumber FABRIC_VCPE_CONNECT_POINT = PortNumber.portNumber(3);
    private static final String FABRIC_CONTROLLER_ADDRESS = "10.0.3.136";
    private static final int FABRIC_SERVER_PORT = 8181;
    private static final String FABRIC_BASE_URI = "/onos/cordfabric/vlans/add";

    private static final DeviceId OLT_DEVICE_ID = DeviceId.deviceId("of:90e2ba82f97791e9");
    private static final int OLT_UPLINK_PORT = 129;

    private static final ConnectPoint FABRIC_PORT = new ConnectPoint(
            DeviceId.deviceId("of:000090e2ba82f974"),
            PortNumber.portNumber(2));

    private final Logger log = getLogger(getClass());
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Property(name = XOS_SERVER_ADDRESS_PROPERTY_NAME,
              value = TEST_XOS_SERVER_ADDRESS,
              label = "XOS Server address")
    protected String xosServerAddress = TEST_XOS_SERVER_ADDRESS;

    @Property(name = XOS_SERVER_PORT_PROPERTY_NAME,
              intValue = TEST_XOS_SERVER_PORT,
              label = "XOS Server port")
    protected int xosServerPort = TEST_XOS_SERVER_PORT;

    @Property(name = XOS_PROVIDER_SERVICE_PROPERTY_NAME,
            intValue = TEST_XOS_PROVIDER_SERVICE,
            label = "XOS Provider Service")
    protected int xosProviderService = TEST_XOS_PROVIDER_SERVICE;

    private ApplicationId appId;
    private Map<String, ConnectPoint> nodeToPort;
    private Map<Long, Short> portToVlan;
    private Map<ConnectPoint, String> portToSsid;

    @Activate
    public void activate(ComponentContext context) {
        log.info("XOS app is starting");
        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("org.onosproject.xosintegration");

        setupMap();

        readComponentConfiguration(context);

        log.info("XOS({}) started", appId.id());
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        log.info("XOS({}) stopped", appId.id());
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);
    }

    private void setupMap() {
        nodeToPort = Maps.newHashMap();

        nodeToPort.put("cordcompute01.onlab.us", new ConnectPoint(FABRIC_DEVICE_ID,
                                                                  PortNumber.portNumber(4)));

        nodeToPort.put("cordcompute02.onlab.us", new ConnectPoint(FABRIC_DEVICE_ID,
                                                                  PortNumber.portNumber(3)));

        portToVlan = Maps.newHashMap();
        portToVlan.putIfAbsent(1L, (short) 201);
        portToVlan.putIfAbsent(6L, (short) 401);

        portToSsid = Maps.newHashMap();
        portToSsid.put(new ConnectPoint(OLT_DEVICE_ID, PortNumber.portNumber(1)), "0");
        portToSsid.put(new ConnectPoint(FABRIC_DEVICE_ID, PortNumber.portNumber(6)), "1");
    }

    /**
     * Converts a JSON representation of a tenant into a tenant object.
     *
     * @param jsonTenant JSON object representing the tenant
     * @return volt tenant object
     */
    private VoltTenant jsonToTenant(JsonObject jsonTenant) {
        return VoltTenant.builder()
                .withHumanReadableName(jsonTenant.get("humanReadableName").asString())
                .withId(jsonTenant.get("id").asInt())
                .withProviderService(jsonTenant.get("provider_service").asInt())
                .withServiceSpecificId(jsonTenant.get("service_specific_id").asString())
                .withVlanId(jsonTenant.get("vlan_id").asString())
                .build();
    }

    /**
     * Converts a tenant object into a JSON string.
     *
     * @param tenant volt tenant object to convert
     * @return JSON string for the tenant
     */
    private String tenantToJson(VoltTenant tenant) {
        return "{"
                    + "\"humanReadableName\": \"" + tenant.humanReadableName() + "\","
                    + "\"id\": \"" + tenant.id() + "\","
                    + "\"provider_service\": \"" + tenant.providerService() + "\","
                    + "\"service_specific_id\": \"" + tenant.serviceSpecificId() + "\","
                    + "\"vlan_id\": \"" + tenant.vlanId() + "\""
                    + "}";
    }

    /**
     * Gets a client web resource builder for the base XOS REST API
     * with no additional URI.
     *
     * @return web resource builder
     * @deprecated in Cardinal Release
     */
    @Deprecated
    private Invocation.Builder getClientBuilder() {
        return getClientBuilder("");
    }

    /**
     * Gets a client web resource builder for the base XOS REST API
     * with an optional additional URI.
     *
     * @return web resource builder
     * @deprecated in Cardinal Release
     */
    @Deprecated
    private Invocation.Builder getClientBuilder(String uri) {
        String baseUrl = "http://" + xosServerAddress + ":"
                + Integer.toString(xosServerPort);
        Client client = ClientBuilder.newClient();
        client.register(HttpAuthenticationFeature.basic("padmin@vicci.org", "letmein"));
        WebTarget wt = client.target(baseUrl
                + XOS_TENANT_BASE_URI + uri);
        return wt.request(JSON_UTF_8.toString());
    }

    /**
     * Performs a REST GET operation on the base XOS REST URI.
     *
     * @return JSON string fetched by the GET operation
     * @deprecated in Cardinal Release
     */
    @Deprecated
    private String getRest() {
        return getRest("");
    }

    /**
     * Performs a REST GET operation on the base XOS REST URI with
     * an optional additional URI.
     *
     * @return JSON string fetched by the GET operation
     * @deprecated in Cardinal Release
     */
    @Deprecated
    private String getRest(String uri) {
        Invocation.Builder builder = getClientBuilder(uri);
        Response response = builder.get();

        if (response.getStatus() != HTTP_OK) {
            log.info("REST GET request returned error code {}",
                    response.getStatus());
        }
        String jsonString = builder.get(String.class);
        log.info("JSON read:\n{}", jsonString);

        return jsonString;
    }

    /**
     * Performs a REST POST operation of a json string on the base
     * XOS REST URI with an optional additional URI.
     *
     * @param json JSON string to post
     * @deprecated in Cardinal Release
     */
    @Deprecated
    private String postRest(String json) {
        Invocation.Builder builder = getClientBuilder();
        Response response = builder.post(Entity.json(json));

        if (response.getStatus() != HTTP_CREATED) {
            log.info("REST POST request returned error code {}",
                    response.getStatus());
        }
        return builder.post(Entity.json(json), String.class);
    }

    /**
     * Performs a REST DELETE operation on the base
     * XOS REST URI with an optional additional URI.
     *
     * @param uri optional additional URI
     * @deprecated in Cardinal Release
     */
    @Deprecated
    private void deleteRest(String uri) {
        Invocation.Builder builder = getClientBuilder(uri);
        Response response = builder.delete();

        if (response.getStatus() != HTTP_NO_CONTENT) {
            log.info("REST DELETE request returned error code {}",
                    response.getStatus());
        }
    }

    /**
     * Deletes the tenant with the given ID.
     *
     * @param tenantId ID of tenant to delete
     */
    private void deleteTenant(long tenantId) {
        deleteRest(Long.toString(tenantId));
    }

    @Override
    public Set<VoltTenant> getAllTenants() {
        String jsonString = getRest();

        JsonArray voltTenantItems = JsonArray.readFrom(jsonString);

        return IntStream.range(0, voltTenantItems.size())
                .mapToObj(index -> jsonToTenant(voltTenantItems.get(index).asObject()))
                .collect(Collectors.toSet());
    }

    @Override
    public void removeTenant(long id) {
        deleteTenant(id);
    }

    @Override
    public VoltTenant addTenant(VoltTenant newTenant) {
        long providerServiceId = newTenant.providerService();
        if (providerServiceId == -1) {
            providerServiceId = xosProviderService;
        }

        PortNumber onuPort = newTenant.port().port();
        VlanId subscriberVlan;
        try {
            subscriberVlan = VlanId.vlanId(portToVlan.get(onuPort.toLong()));
        } catch (NullPointerException npe) {
            log.error("No matched port in portToVlan map", npe);
            return newTenant;
        }

        VoltTenant tenantToCreate = VoltTenant.builder()
                .withProviderService(providerServiceId)
                .withServiceSpecificId(portToSsid.get(newTenant.port()))
                .withVlanId(String.valueOf(subscriberVlan.toShort()))
                .withPort(newTenant.port())
                .build();
        String json = tenantToJson(tenantToCreate);


        provisionVlanOnPort(OLT_DEVICE_ID, OLT_UPLINK_PORT, onuPort, subscriberVlan.toShort());

        String retJson = postRest(json);

        fetchCpeLocation(tenantToCreate, retJson);

        return newTenant;
    }

    private void fetchCpeLocation(VoltTenant newTenant, String jsonString) {
        JsonObject json = JsonObject.readFrom(jsonString);

        if (json.get("computeNodeName") != null) {
            ConnectPoint point = nodeToPort.get(json.get("computeNodeName").asString());
            //ConnectPoint fromPoint = newTenant.port();
            ConnectPoint oltPort = new ConnectPoint(FABRIC_DEVICE_ID, FABRIC_OLT_CONNECT_POINT);

            provisionFabric(VlanId.vlanId(Short.parseShort(newTenant.vlanId())),
                            point, oltPort);
        }

    }

    @Override
    public VoltTenant getTenant(long id) {
        String jsonString = getRest(Long.toString(id));
        JsonObject jsonTenant = JsonObject.readFrom(jsonString);
        if (jsonTenant.get("id") != null) {
            return jsonToTenant(jsonTenant);
        } else {
            return null;
        }
    }

    private void provisionVlanOnPort(DeviceId deviceId, int uplinkPort, PortNumber p, short vlanId) {

        TrafficSelector upstream = DefaultTrafficSelector.builder()
                .matchVlanId(VlanId.ANY)
                .matchInPort(p)
                .build();

        TrafficSelector downstream = DefaultTrafficSelector.builder()
                .matchVlanId(VlanId.vlanId(vlanId))
                .matchInPort(PortNumber.portNumber(uplinkPort))
                .build();

        TrafficTreatment upstreamTreatment = DefaultTrafficTreatment.builder()
                .setVlanId(VlanId.vlanId(vlanId))
                .setOutput(PortNumber.portNumber(uplinkPort))
                .build();

        TrafficTreatment downstreamTreatment = DefaultTrafficTreatment.builder()
                .popVlan()
                .setOutput(p)
                .build();


        ForwardingObjective upFwd = DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(1000)
                .makePermanent()
                .withSelector(upstream)
                .fromApp(appId)
                .withTreatment(upstreamTreatment)
                .add();

        ForwardingObjective downFwd = DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(1000)
                .makePermanent()
                .withSelector(downstream)
                .fromApp(appId)
                .withTreatment(downstreamTreatment)
                .add();

        flowObjectiveService.forward(deviceId, upFwd);
        flowObjectiveService.forward(deviceId, downFwd);

    }

    private void provisionDataPlane(VoltTenant tenant) {
        VlanId vlan = VlanId.vlanId(Short.parseShort(tenant.vlanId()));

        TrafficSelector fromGateway = DefaultTrafficSelector.builder()
                .matchInPhyPort(tenant.port().port())
                .build();

        TrafficSelector fromFabric = DefaultTrafficSelector.builder()
                .matchInPhyPort(FABRIC_PORT.port())
                .matchVlanId(vlan)
                .build();

        TrafficTreatment toFabric = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(vlan)
                .setOutput(FABRIC_PORT.port())
                .build();

        TrafficTreatment toGateway = DefaultTrafficTreatment.builder()
                .popVlan()
                .setOutput(tenant.port().port())
                .build();

        ForwardingObjective forwardToFabric = DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .makePermanent()
                .fromApp(appId)
                .withSelector(fromGateway)
                .withTreatment(toFabric)
                .add();

        ForwardingObjective forwardToGateway = DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .makePermanent()
                .fromApp(appId)
                .withSelector(fromFabric)
                .withTreatment(toGateway)
                .add();

        flowObjectiveService.forward(FABRIC_PORT.deviceId(), forwardToFabric);
        flowObjectiveService.forward(FABRIC_PORT.deviceId(), forwardToGateway);
    }

    private void provisionFabric(VlanId vlanId, ConnectPoint point, ConnectPoint fromPoint) {

        long vlan = vlanId.toShort();

        JsonObject node = new JsonObject();
        node.add("vlan", vlan);
        if (vlan == 201) {
            node.add("iptv", true);
        } else {
            node.add("iptv", false);
        }
        JsonArray array = new JsonArray();
        JsonObject cp1 = new JsonObject();
        JsonObject cp2 = new JsonObject();
        cp1.add("device", point.deviceId().toString());
        cp1.add("port", point.port().toLong());
        cp2.add("device", fromPoint.deviceId().toString());
        cp2.add("port", fromPoint.port().toLong());
        array.add(cp1);
        array.add(cp2);
        node.add("ports", array);


        String baseUrl = "http://" + FABRIC_CONTROLLER_ADDRESS + ":"
                + Integer.toString(FABRIC_SERVER_PORT);
        Client client = ClientBuilder.newClient();
        WebTarget wt = client.target(baseUrl + FABRIC_BASE_URI);
        Invocation.Builder builder = wt.request(JSON_UTF_8.toString());

        builder.post(Entity.json(node.toString()));
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String newXosServerAddress =
                Tools.get(properties, XOS_SERVER_ADDRESS_PROPERTY_NAME);
        if (!isNullOrEmpty(newXosServerAddress)) {
            xosServerAddress = newXosServerAddress;
        }

        String newXosServerPortString =
                Tools.get(properties, XOS_SERVER_PORT_PROPERTY_NAME);
        if (!isNullOrEmpty(newXosServerPortString)) {
            xosServerPort = Integer.parseInt(newXosServerPortString);
        }

        String newXosProviderServiceString =
                Tools.get(properties, XOS_PROVIDER_SERVICE_PROPERTY_NAME);
        if (!isNullOrEmpty(newXosProviderServiceString)) {
            xosProviderService = Integer.parseInt(newXosProviderServiceString);
        }
    }
}


