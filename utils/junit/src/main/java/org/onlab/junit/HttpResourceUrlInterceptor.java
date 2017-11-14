/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onlab.junit;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * Intercepts HTTP URL connections and supplies predefined data from a resource. Used for supplying data to HTTP
 * connections in unit tests.
 */
public class HttpResourceUrlInterceptor {

    /**
     * Handles creation of HTTP Connections to the resource data.
     */
    private static class HttpResourceUrlInterceptorHandler extends URLStreamHandler {

        String resourceName;

        HttpResourceUrlInterceptorHandler(String resourceName) {
            this.resourceName = resourceName;
        }

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new InterceptedHttpUrlConnection(u, resourceName);
        }
    }

    /**
     * Creates stream handlers for the interceptor.
     */
    public static class HttpResourceUrlInterceptorFactory implements URLStreamHandlerFactory {

        String resourceName;

        public HttpResourceUrlInterceptorFactory(String resourceName) {
            this.resourceName = resourceName;
        }

        @Override
        public URLStreamHandler createURLStreamHandler(String protocol) {
            return new HttpResourceUrlInterceptorHandler(resourceName);
        }
    }

    /**
     * HTTP Url Connection that is backed by the data in the resource.
     */
    private static final class InterceptedHttpUrlConnection extends HttpURLConnection {

        private final String resourceName;

        private InterceptedHttpUrlConnection(URL url, String resourceName) {
            super(url);
            this.resourceName = resourceName;
        }

        @Override
        public int getResponseCode() {
            return HTTP_OK;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void disconnect() {
            // noop
        }

        @Override
        public void connect() {
            // noop
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return this.getClass().getResource(resourceName).openStream();
        }

    }
}


