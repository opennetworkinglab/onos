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

package org.onosproject.protocol.http.ctl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.protocol.http.HttpSBController;
import org.onosproject.protocol.rest.RestSBDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * The implementation of HttpSBController.
 */
public class HttpSBControllerImpl implements HttpSBController {

    private static final Logger log = LoggerFactory.getLogger(HttpSBControllerImpl.class);
    private static final String XML = "xml";
    private static final String JSON = "json";
    protected static final String DOUBLESLASH = "//";
    protected static final String COLON = ":";
    private static final int STATUS_OK = Response.Status.OK.getStatusCode();
    private static final int STATUS_CREATED = Response.Status.CREATED.getStatusCode();
    private static final int STATUS_ACCEPTED = Response.Status.ACCEPTED.getStatusCode();
    private static final String HTTPS = "https";
    private static final String AUTHORIZATION_PROPERTY = "authorization";
    private static final String BASIC_AUTH_PREFIX = "Basic ";

    private final Map<DeviceId, RestSBDevice> deviceMap = new ConcurrentHashMap<>();
    private final Map<DeviceId, Client> clientMap = new ConcurrentHashMap<>();

    public Map<DeviceId, RestSBDevice> getDeviceMap() {
        return deviceMap;
    }

    public Map<DeviceId, Client> getClientMap() {
        return clientMap;
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
        return deviceMap.values().stream().filter(v -> v.ip().equals(ip) && v.port() == port).findFirst().get();
    }

    @Override
    public void addDevice(RestSBDevice device) {
        if (!deviceMap.containsKey(device.deviceId())) {
            Client client = ignoreSslClient();
            if (device.username() != null) {
                String username = device.username();
                String password = device.password() == null ? "" : device.password();
                authenticate(client, username, password);
            }
            clientMap.put(device.deviceId(), client);
            deviceMap.put(device.deviceId(), device);
        } else {
            log.warn("Trying to add a device that is already existing {}", device.deviceId());
        }

    }

    @Override
    public void removeDevice(DeviceId deviceId) {
        clientMap.remove(deviceId);
        deviceMap.remove(deviceId);
    }

    @Override
    public boolean post(DeviceId device, String request, InputStream payload, String mediaType) {
        return checkStatusCode(post(device, request, payload, typeOfMediaType(mediaType)));
    }

    @Override
    public int post(DeviceId device, String request, InputStream payload, MediaType mediaType) {
        Response response = getResponse(device, request, payload, mediaType);
        if (response == null) {
            return Status.NO_CONTENT.getStatusCode();
        }
        return response.getStatus();
    }

    @Override
    public <T> T post(DeviceId device, String request, InputStream payload, String mediaType, Class<T> responseClass) {
        return post(device, request, payload, typeOfMediaType(mediaType), responseClass);
    }

    @Override
    public <T> T post(DeviceId device, String request, InputStream payload, MediaType mediaType,
                      Class<T> responseClass) {
        Response response = getResponse(device, request, payload, mediaType);
        if (response != null && response.hasEntity()) {
            return response.readEntity(responseClass);
        }
        log.error("Response from device {} for request {} contains no entity", device, request);
        return null;
    }

    private Response getResponse(DeviceId device, String request, InputStream payload, MediaType mediaType) {

        WebTarget wt = getWebTarget(device, request);

        Response response = null;
        if (payload != null) {
            try {
                response = wt.request(mediaType)
                        .post(Entity.entity(IOUtils.toString(payload, StandardCharsets.UTF_8), mediaType));
            } catch (IOException e) {
                log.error("Cannot do POST {} request on device {} because can't read payload", request, device);
            }
        } else {
            response = wt.request(mediaType).post(Entity.entity(null, mediaType));
        }
        return response;
    }

    @Override
    public boolean put(DeviceId device, String request, InputStream payload, String mediaType) {
        return checkStatusCode(put(device, request, payload, typeOfMediaType(mediaType)));
    }

    @Override
    public int put(DeviceId device, String request, InputStream payload, MediaType mediaType) {

        WebTarget wt = getWebTarget(device, request);

        Response response = null;
        if (payload != null) {
            try {
                response = wt.request(mediaType).put(Entity.entity(IOUtils.
                        toString(payload, StandardCharsets.UTF_8), mediaType));
            } catch (IOException e) {
                log.error("Cannot do PUT {} request on device {} because can't read payload", request, device);
            }
        } else {
            response = wt.request(mediaType).put(Entity.entity(null, mediaType));
        }

        if (response == null) {
            return Status.NO_CONTENT.getStatusCode();
        }
        return response.getStatus();
    }

    @Override
    public InputStream get(DeviceId device, String request, String mediaType) {
        return get(device, request, typeOfMediaType(mediaType));
    }

    @Override
    public InputStream get(DeviceId device, String request, MediaType mediaType) {
        WebTarget wt = getWebTarget(device, request);

        Response s = wt.request(mediaType).get();

        if (checkReply(s)) {
            return new ByteArrayInputStream(s.readEntity((String.class)).getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    @Override
    public boolean patch(DeviceId device, String request, InputStream payload, String mediaType) {
        return checkStatusCode(patch(device, request, payload, typeOfMediaType(mediaType)));
    }

    @Override
    public int patch(DeviceId device, String request, InputStream payload, MediaType mediaType) {

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
                input.setContentType(mediaType.toString());
                httprequest.setEntity(input);
            }
            CloseableHttpClient httpClient;
            if (deviceMap.containsKey(device) && deviceMap.get(device).protocol().equals(HTTPS)) {
                httpClient = getApacheSslBypassClient();
            } else {
                httpClient = HttpClients.createDefault();
            }
            return httpClient.execute(httprequest).getStatusLine().getStatusCode();
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("Cannot do PATCH {} request on device {}", request, device, e);
        }
        return Status.BAD_REQUEST.getStatusCode();
    }

    @Override
    public boolean delete(DeviceId device, String request, InputStream payload, String mediaType) {
        return checkStatusCode(delete(device, request, payload, typeOfMediaType(mediaType)));
    }

    @Override
    public int delete(DeviceId device, String request, InputStream payload, MediaType mediaType) {

        WebTarget wt = getWebTarget(device, request);

        // FIXME: do we need to delete an entry by enclosing data in DELETE
        // request?
        // wouldn't it be nice to use PUT to implement the similar concept?
        Response response = wt.request(mediaType).delete();

        return response.getStatus();
    }

    private MediaType typeOfMediaType(String type) {
        switch (type) {
        case XML:
            return MediaType.APPLICATION_XML_TYPE;
        case JSON:
            return MediaType.APPLICATION_JSON_TYPE;
        default:
            throw new IllegalArgumentException("Unsupported media type " + type);

        }
    }

    private void authenticate(Client client, String username, String password) {
        client.register(HttpAuthenticationFeature.basic(username, password));
    }

    protected WebTarget getWebTarget(DeviceId device, String request) {
        log.debug("Sending request to URL {} ", getUrlString(device, request));
        return clientMap.get(device).target(getUrlString(device, request));
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

    protected String getUrlString(DeviceId device, String request) {
        if (deviceMap.get(device).url() != null) {
            return deviceMap.get(device).protocol() + COLON + DOUBLESLASH + deviceMap.get(device).url() + request;
        } else {
            return deviceMap.get(device).protocol() + COLON + DOUBLESLASH + deviceMap.get(device).ip().toString()
                    + COLON + deviceMap.get(device).port() + request;
        }
    }

    private boolean checkReply(Response response) {
        if (response != null) {
            boolean statusCode = checkStatusCode(response.getStatus());
            if (!statusCode && response.hasEntity()) {
                log.error("Failed request, HTTP error msg : " + response.readEntity(String.class));
            }
            return statusCode;
        }
        log.error("Null reply from device");
        return false;
    }

    private boolean checkStatusCode(int statusCode) {
        if (statusCode == STATUS_OK || statusCode == STATUS_CREATED || statusCode == STATUS_ACCEPTED) {
            return true;
        } else {
            log.error("Failed request, HTTP error code : " + statusCode);
            return false;
        }
    }

    private Client ignoreSslClient() {
        SSLContext sslcontext = null;

        try {
            sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } }, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        return ClientBuilder.newBuilder().sslContext(sslcontext).hostnameVerifier((s1, s2) -> true).build();
    }

}
