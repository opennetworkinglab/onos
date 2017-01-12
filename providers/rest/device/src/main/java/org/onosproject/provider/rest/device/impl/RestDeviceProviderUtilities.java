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

package org.onosproject.provider.rest.device.impl;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Utilities class for RestDevice provider.
 */
final class RestDeviceProviderUtilities {

    private static final String TLS = "TLS";

    //disable construction.
    private RestDeviceProviderUtilities(){}

    /**
     * Method that bypasses every SSL certificate verification and accepts every
     * connection with any SSL protected device that ONOS has an interaction with.
     * Needs addressing for secutirty purposes.
     *
     * @throws NoSuchAlgorithmException if algorithm specified is not available
     * @throws KeyManagementException   if unable to use the key
     */
    //FIXME redo for security purposes.
    protected static void enableSslCert() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext ctx = SSLContext.getInstance(TLS);
        ctx.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
        SSLContext.setDefault(ctx);
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> {
            //FIXME better way to do this.
            return true;
        });
    }

    //FIXME this accepts every connection
    private static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
