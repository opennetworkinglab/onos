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

import com.google.common.collect.ImmutableMap;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.protocol.rest.RestSBController;
import org.onosproject.protocol.rest.RestSBDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
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
    private static final String XML = "xml";
    private static final String JSON = "json";
    private static final String DOUBLESLASH = "//";
    private static final String COLON = ":";
    private static final int STATUS_OK = Response.Status.OK.getStatusCode();
    private static final int STATUS_CREATED = Response.Status.CREATED.getStatusCode();
    private static final int STATUS_ACCEPTED = Response.Status.ACCEPTED.getStatusCode();
    private static final String HTTPS = "https";
    private static final String AUTHORIZATION_PROPERTY = "authorization";
    private static final String BASIC_AUTH_PREFIX = "Basic ";

    private final Map<DeviceId, RestSBDevice> deviceMap = new ConcurrentHashMap<>();
    Client client;

    @Activate
    public void activate() {
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
        return ImmutableMap.copyOf(deviceMap);
    }

    @Override
    public RestSBDevice getDevice(DeviceId deviceInfo) {
        return deviceMap.get(deviceInfo);
    }

    @Override
    public RestSBDevice getDevice(IpAddress ip, int port) {
        return deviceMap.values().stream().filter(v -> v.ip().equals(ip)
                && v.port() == port).findFirst().get();
    }

    @Override
    public void addDevice(RestSBDevice device) {
        deviceMap.put(device.deviceId(), device);
    }

    @Override
    public void removeDevice(DeviceId deviceId) {
        deviceMap.remove(deviceId);
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

        ClientResponse s = webResource.accept(type).get(ClientResponse.class);
        if (checkReply(s)) {
            return new ByteArrayInputStream(s.getEntity(String.class)
                                                    .getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    @Override
    public boolean patch(DeviceId device, String request, InputStream payload, String mediaType) {
        try {
            log.debug("Url request {} ", getUrlString(device, request));
            HttpPatch httprequest = new HttpPatch(getUrlString(device, request));
            if (deviceMap.get(device).username() != null) {
                String pwd = deviceMap.get(device).password() == null ? "" : COLON + deviceMap.get(device).password();
                String userPassword = deviceMap.get(device).username() + pwd;
                String base64string = Base64.getEncoder().encodeToString(userPassword.getBytes(StandardCharsets.UTF_8));
                httprequest.addHeader(AUTHORIZATION_PROPERTY, BASIC_AUTH_PREFIX + base64string);
            }
            if (payload != null) {
                StringEntity input = new StringEntity(IOUtils.toString(payload, StandardCharsets.UTF_8));
                input.setContentType(mediaType);
                httprequest.setEntity(input);
            }
            CloseableHttpClient httpClient;
            if (deviceMap.containsKey(device) && deviceMap.get(device).protocol().equals(HTTPS)) {
                httpClient = getApacheSslBypassClient();
            } else {
                httpClient = HttpClients.createDefault();
            }
            int responseStatusCode = httpClient
                    .execute(httprequest)
                    .getStatusLine()
                    .getStatusCode();
            return checkStatusCode(responseStatusCode);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("Cannot do PATCH {} request on device {}",
                      request, device, e);
        }
        return false;
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
        log.debug("Sending request to URL {} ", getUrlString(device, request));
        WebResource webResource = client.resource(getUrlString(device, request));
        if (deviceMap.containsKey(device) && deviceMap.get(device).username() != null) {
            client.addFilter(new HTTPBasicAuthFilter(deviceMap.get(device).username(),
                                                     deviceMap.get(device).password() == null ?
                                                             "" : deviceMap.get(device).password()));
        }
        return webResource;
    }

    //FIXME security issue: this trusts every SSL certificate, even if is self-signed. Also deprecated methods.
    private CloseableHttpClient getApacheSslBypassClient() throws NoSuchAlgorithmException,
            KeyManagementException, KeyStoreException {
        return HttpClients.custom().
                setHostnameVerifier(new AllowAllHostnameVerifier()).
                setSslcontext(new SSLContextBuilder()
                                      .loadTrustMaterial(null, (arg0, arg1) -> true)
                                      .build()).build();
    }

    private String getUrlString(DeviceId device, String request) {
        if (deviceMap.get(device).url() != null) {
            return deviceMap.get(device).protocol() + COLON + DOUBLESLASH
                    + deviceMap.get(device).url() + request;
        } else {
            return deviceMap.get(device).protocol() + COLON +
                    DOUBLESLASH +
                    deviceMap.get(device).ip().toString() +
                    COLON + deviceMap.get(device).port() + request;
        }
    }

    private boolean checkReply(ClientResponse response) {
        if (response != null) {
            return checkStatusCode(response.getStatus());
        }
        log.error("Null reply from device");
        return false;
    }

    private boolean checkStatusCode(int statusCode) {
        if (statusCode == STATUS_OK ||
                statusCode == STATUS_CREATED ||
                statusCode == STATUS_ACCEPTED) {
            return true;
        } else {
            log.error("Failed request, HTTP error code : "
                              + statusCode);
            return false;
        }
    }
}
