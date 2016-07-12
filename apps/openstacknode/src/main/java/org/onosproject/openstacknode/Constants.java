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
package org.onosproject.openstacknode;

/**
 * Provides constants used in OpenStack node services.
 */
public final class Constants {

    private Constants() {
    }

    public static final String INTEGRATION_BRIDGE = "br-int";
    public static final String ROUTER_BRIDGE = "br-router";
    public static final String DEFAULT_TUNNEL = "vxlan";
    public static final String PATCH_INTG_BRIDGE = "patch-intg";
    public static final String PATCH_ROUT_BRIDGE = "patch-rout";

    public static final int DEFAULT_OVSDB_PORT = 6640;
    public static final int DEFAULT_OFPORT = 6653;
    public static final String DEFAULT_OF_PROTO = "tcp";
}