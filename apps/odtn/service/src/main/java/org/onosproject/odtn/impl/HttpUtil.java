/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.annotations.Beta;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * The implementation of HttpUtils.
 */
@Beta
public class HttpUtil {

    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);
    private static final String XML = "xml";
    private static final String JSON = "json";
    protected static final String DOUBLESLASH = "//";
    protected static final String COLON = ":";
    private static final int STATUS_OK = Response.Status.OK.getStatusCode();
    private static final int STATUS_CREATED = Response.Status.CREATED.getStatusCode();
    private static final int STATUS_ACCEPTED = Response.Status.ACCEPTED.getStatusCode();
    private static final String HTTPS = "https";

    private Client client = null;
    private String protocol = null;
    private String ip = null;
    private String port = null;

    public HttpUtil(String protocol, String ip, String port) {
        //TODO check not null
        this.protocol = protocol.equals("") ? HTTPS : protocol;
        this.ip = ip;
        this.port = port;
    }

    public void connect(String username, String password) {
        client = ignoreSslClient();
        authenticate(client, username, password);
    }

    public void disconnect() {
        protocol = "";
        ip = "";
        port = "";
        client = null;
    }

    public int post(String request, InputStream payload, MediaType mediaType) {
        Response response = getResponse(request, payload, mediaType);
        if (response == null) {
            return Status.NO_CONTENT.getStatusCode();
        }
        return response.getStatus();
    }

    public <T> T post(DeviceId device, String request, InputStream payload, MediaType mediaType,
                      Class<T> responseClass) {
        Response response = getResponse(request, payload, mediaType);
        if (response != null && response.hasEntity()) {
            // Do not read the entity if the responseClass is of type Response. This would allow the
            // caller to receive the Response directly and try to read its appropriate entity locally.
            return responseClass == Response.class ? (T) response : response.readEntity(responseClass);
        }
        log.error("Response from device {} for request {} contains no entity", device, request);
        return null;
    }

    private Response getResponse(String request, InputStream payload, MediaType mediaType) {

        WebTarget wt = getWebTarget(request);

        Response response = null;
        if (payload != null) {
            try {
                response = wt.request(mediaType)
                        .post(Entity.entity(IOUtils.toString(payload, StandardCharsets.UTF_8), mediaType));
            } catch (IOException e) {
                log.error("Cannot do POST {} request on GNPY because can't read payload", request);
            }
        } else {
            response = wt.request(mediaType).post(Entity.entity(null, mediaType));
        }
        return response;
    }

    public int put(String request, InputStream payload, MediaType mediaType) {

        WebTarget wt = getWebTarget(request);

        Response response = null;
        if (payload != null) {
            try {
                response = wt.request(mediaType).put(Entity.entity(IOUtils.
                        toString(payload, StandardCharsets.UTF_8), mediaType));
            } catch (IOException e) {
                log.error("Cannot do POST {} request on GNPY because can't read payload", request);
            }
        } else {
            response = wt.request(mediaType).put(Entity.entity(null, mediaType));
        }

        if (response == null) {
            return Status.NO_CONTENT.getStatusCode();
        }
        return response.getStatus();
    }

    public InputStream get(String request, MediaType mediaType) {
        WebTarget wt = getWebTarget(request);

        Response s = wt.request(mediaType).get();

        if (checkReply(s)) {
            return new ByteArrayInputStream(s.readEntity((String.class)).getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    public int delete(DeviceId device, String request, InputStream payload, MediaType mediaType) {

        WebTarget wt = getWebTarget(request);

        // FIXME: do we need to delete an entry by enclosing data in DELETE
        // request?
        // wouldn't it be nice to use PUT to implement the similar concept?
        Response response = null;
        try {
            response = wt.request(mediaType).delete();
        } catch (ProcessingException procEx) {
            log.error("Cannot issue DELETE {} request on device {}", request, device);
            return Status.SERVICE_UNAVAILABLE.getStatusCode();
        }

        return response.getStatus();
    }

    private void authenticate(Client client, String username, String password) {
        client.register(HttpAuthenticationFeature.basic(username, password));

    }

    protected WebTarget getWebTarget(String request) {
        log.debug("Sending request to URL {} ", getUrlString(request));
        return client.target(getUrlString(request));
    }

    protected String getUrlString(String request) {
        return protocol + COLON + DOUBLESLASH + ip + COLON + port + request;
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
                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } }, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException(e);
        }

        return ClientBuilder.newBuilder().sslContext(sslcontext).hostnameVerifier((s1, s2) -> true).build();
    }

}
