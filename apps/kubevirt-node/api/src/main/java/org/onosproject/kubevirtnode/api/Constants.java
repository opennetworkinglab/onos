/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.api;

/**
 * Provides constants used in KubeVirt node services.
 */
public final class Constants {

    private Constants() {
    }

    public static final String HOST_NAME = "hostname";
    public static final String TYPE = "type";
    public static final String MANAGEMENT_IP = "managementIp";
    public static final String DATA_IP = "dataIp";

    public static final String VXLAN = "vxlan";
    public static final String GRE = "gre";
    public static final String GENEVE = "geneve";
    public static final String STT = "stt";

    public static final String INTEGRATION_BRIDGE = "br-int";
    public static final String TUNNEL_BRIDGE = "br-tun";
    public static final String TENANT_BRIDGE_PREFIX = "br-int-";

    public static final String INTEGRATION_TO_TUNNEL = "int-to-tun";
    public static final String TUNNEL_TO_INTEGRATION = "tun-to-int";

    public static final String BRIDGE_PREFIX = "br-";
    public static final String INTEGRATION_TO_PHYSICAL_PREFIX = "int-to-";
    public static final String PHYSICAL_TO_INTEGRATION_SUFFIX = "-to-int";

    public static final String FLOW_KEY = "flow";

    public static final String DEFAULT_CLUSTER_NAME = "default";

    public static final String SONA_PROJECT_DOMAIN = "sonaproject.github.io";
}
