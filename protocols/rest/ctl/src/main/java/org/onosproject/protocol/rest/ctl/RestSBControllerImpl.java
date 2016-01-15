/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.protocol.rest.ctl;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.protocol.rest.RestSBController;
import org.onosproject.protocol.rest.RestSBDevice;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The implementation of RestSBController.
 */
@Component(immediate = true)
@Service
public class RestSBControllerImpl implements RestSBController {

    private static final Logger log =
            LoggerFactory.getLogger(RestSBControllerImpl.class);
    private static final String APPLICATION = "application/";
    private static final String XML = "xml";
    private static final String JSON = "json";
    private static final String DOUBLESLASH = "//";
    private static final String COLON = ":";
    private static final int STATUS_OK = Response.Status.OK.getStatusCode();
    private static final int STATUS_CREATED = Response.Status.CREATED.getStatusCode();
    private static final int STATUS_ACCEPTED = Response.Status.ACCEPTED.getStatusCode();
    private static final String SLASH = "/";

    private final Map<DeviceId, RestSBDevice> deviceMap = new ConcurrentHashMap<>();
    Client client;

    @Activate
    public void activate(ComponentContext context) {
        client = Client.create();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deviceMap.clear();
        log.info("Stopped");
    }

    @Override
    public Map<DeviceId, RestSBDevice> getDevices() {
        return deviceMap;
    }

    @Override
    public RestSBDevice getDevice(DeviceId deviceInfo) {
        return deviceMap.get(deviceInfo);
    }

    @Override
    public RestSBDevice getDevice(IpAddress ip, int port) {
        for (DeviceId info : deviceMap.keySet()) {
            if (IpAddress.valueOf(info.uri().getHost()).equals(ip) &&
                    info.uri().getPort() == port) {
                return deviceMap.get(info);
            }
        }
        return null;
    }

    @Override
    public void addDevice(RestSBDevice device) {
        deviceMap.put(device.deviceId(), device);
    }

    @Override
    public void removeDevice(RestSBDevice device) {
        deviceMap.remove(device.deviceId());
    }

    @Override
    public boolean post(DeviceId device, String request, InputStream payload, String mediaType) {
        WebResource webResource = getWebResource(device, request);

        ClientResponse response = null;
        if (payload != null) {
            try {
                response = webResource.accept(mediaType)
                        .post(ClientResponse.class, IOUtils.toString(payload, StandardCharsets.UTF_8));
            } catch (IOException e) {
                log.error("Cannot do POST {} request on device {} because can't read payload",
                          request, device);
            }
        } else {
            response = webResource.accept(mediaType)
                    .post(ClientResponse.class);
        }
        return checkReply(response);
    }

    @Override
    public boolean put(DeviceId device, String request, InputStream payload, String mediaType) {

        WebResource webResource = getWebResource(device, request);
        ClientResponse response = null;
        if (payload != null) {
            try {
                response = webResource.accept(mediaType)
                        .put(ClientResponse.class, IOUtils.toString(payload, StandardCharsets.UTF_8));
            } catch (IOException e) {
                log.error("Cannot do PUT {} request on device {} because can't read payload",
                          request, device);
            }
        } else {
            response = webResource.accept(mediaType)
                    .put(ClientResponse.class);
        }
        return checkReply(response);
    }

    @Override
    public InputStream get(DeviceId device, String request, String mediaType) {
        WebResource webResource = getWebResource(device, request);
        String type;
        switch (mediaType) {
            case XML:
                type = MediaType.APPLICATION_XML;
                break;
            case JSON:
                type = MediaType.APPLICATION_JSON;
                break;
            default:
                throw new IllegalArgumentException("Unsupported media type " + mediaType);

        }
        return new ByteArrayInputStream(webResource.accept(type).get(ClientResponse.class)
                                                .getEntity(String.class)
                                                .getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean delete(DeviceId device, String request, InputStream payload, String mediaType) {
        WebResource webResource = getWebResource(device, request);
        ClientResponse response = null;
        if (payload != null) {
            try {
                response = webResource.accept(mediaType)
                        .delete(ClientResponse.class, IOUtils.toString(payload, StandardCharsets.UTF_8));
            } catch (IOException e) {
                log.error("Cannot do PUT {} request on device {} because can't read payload",
                          request, device);
            }
        } else {
            response = webResource.accept(mediaType)
                    .delete(ClientResponse.class);
        }
        return checkReply(response);
    }

    private WebResource getWebResource(DeviceId device, String request) {
        return Client.create().resource(deviceMap.get(device).protocol() + COLON +
                                                DOUBLESLASH +
                                                deviceMap.get(device).ip().toString() +
                                                COLON + deviceMap.get(device).port() +
                                                SLASH + request);
    }

    private boolean checkReply(ClientResponse response) {
        if (response != null) {
            if (response.getStatus() == STATUS_OK ||
                    response.getStatus() == STATUS_CREATED ||
                    response.getStatus() == STATUS_ACCEPTED) {
                return true;
            } else {
                log.error("Failed request: HTTP error code : "
                                  + response.getStatus());
                return false;
            }
        }
        log.error("Null reply from device");
        return false;
    }
}
