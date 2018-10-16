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

    //@Property(name = "openflowPorts", value = DEFAULT_OFPORT,
    //        label = "Port numbers (comma separated) used by OpenFlow protocol; default is 6633,6653")
    public static final String OFPORTS = "openflowPorts";
    public static final String OFPORTS_DEFAULT = "6633,6653";

    //@Property(name = "workerThreads", intValue = DEFAULT_WORKER_THREADS,
    //        label = "Number of controller worker threads")
    public static final String WORKER_THREADS = "workerThreads:Integer";
    public static final int WORKER_THREADS_DEFAULT = 0;

    //@Property(name = "tlsMode", value = "",
    //          label = "TLS mode for OpenFlow channel; options are: disabled [default], enabled, strict")
    public static final String TLS_MODE = "tlsMode";
    public static final String TLS_MODE_DEFAULT = "";

    //@Property(name = "keyStore", value = "",
    //        label = "File path to key store for TLS connections")
    public static final String KEY_STORE = "keyStore";
    public static final String KEY_STORE_DEFAULT = "";

    //@Property(name = "keyStorePassword", value = "",
    //        label = "Key store password")
    public static final String KEY_STORE_PASSWORD = "keyStorePassword";
    public static final String KEY_STORE_PASSWORD_DEFAULT = "";

    //@Property(name = "trustStore", value = "",
    //        label = "File path to trust store for TLS connections")
    public static final String TRUST_STORE = "trustStore";
    public static final String TRUST_STORE_DEFAULT = "";

    //@Property(name = "trustStorePassword", value = "",
    //        label = "Trust store password")
    public static final String TRUST_STORE_PASSWORD = "trustStorePassword";
    public static final String TRUST_STORE_PASSWORD_DEFAULT = "";

}
