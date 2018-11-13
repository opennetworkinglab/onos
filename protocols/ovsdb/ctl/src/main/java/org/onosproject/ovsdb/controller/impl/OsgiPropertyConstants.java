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

package org.onosproject.ovsdb.controller.impl;

/**
 * Constants for default values of configurable properties.
 */
public final class OsgiPropertyConstants {

    private OsgiPropertyConstants() {}

    public static final String SERVER_MODE = "";
    public static final boolean SERVER_MODE_DEFAULT = true;

    public static final String OVSDB_TLS_FLAG = "enableOvsdbTls";
    public static final boolean OVSDB_TLS_FLAG_DEFAULT = false;

    public static final String KS_FILE = "keyStoreLocation";
    public static final String KS_FILE_DEFAULT = "../config/onos.jks";

    public static final String TS_FILE = "trustStoreLocation";
    public static final String TS_FILE_DEFAULT = "../config/onos.jks";

    public static final String KS_PASSWORD = "keyStorePassword";
    public static final String KS_PASSWORD_DEFAULT = "222222";

    public static final String TS_PASSWORD = "trustStorePassword";
    public static final String TS_PASSWORD_DEFAULT = "222222";

}
