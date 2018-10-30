/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.openflow.controller.impl;

/**
 * Name/Value constants for properties.
 */

public final class OsgiPropertyConstants {

    private OsgiPropertyConstants() {
    }

    public static final String OFPORTS = "openflowPorts";
    public static final String OFPORTS_DEFAULT = "6633,6653";

    public static final String WORKER_THREADS = "workerThreads";
    public static final int WORKER_THREADS_DEFAULT = 0;

    public static final String TLS_MODE = "tlsMode";
    public static final String TLS_MODE_DEFAULT = "";

    public static final String KEY_STORE = "keyStore";
    public static final String KEY_STORE_DEFAULT = "";

    public static final String KEY_STORE_PASSWORD = "keyStorePassword";
    public static final String KEY_STORE_PASSWORD_DEFAULT = "";

    public static final String TRUST_STORE = "trustStore";
    public static final String TRUST_STORE_DEFAULT = "";

    public static final String TRUST_STORE_PASSWORD = "trustStorePassword";
    public static final String TRUST_STORE_PASSWORD_DEFAULT = "";

}
