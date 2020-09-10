/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snode.api;

/**
 * Provides constants used in Kubernetes node services.
 */
public final class Constants {

    private Constants() {
    }

    public static final String OS_INTEGRATION_BRIDGE = "br-int";
    public static final String INTEGRATION_BRIDGE = "kbr-int";
    public static final String EXTERNAL_BRIDGE = "kbr-ex";
    public static final String LOCAL_BRIDGE = "kbr-local";
    public static final String TUNNEL_BRIDGE = "kbr-tun";
    public static final String ROUTER_BRIDGE = "kbr-router";
    public static final String INTEGRATION_TO_EXTERNAL_BRIDGE = "int-ex";
    public static final String PHYSICAL_EXTERNAL_BRIDGE = "ex-int";
    public static final String INTEGRATION_TO_LOCAL_BRIDGE = "int-local";
    public static final String LOCAL_TO_INTEGRATION_BRIDGE = "local-int";
    public static final String EXTERNAL_TO_ROUTER = "ex-router";
    public static final String ROUTER_TO_EXTERNAL = "router-ex";
    public static final String ROUTER = "router";
    public static final String INTEGRATION_TO_TUN_BRIDGE = "int-tun";
    public static final String TUN_TO_INTEGRATION_BRIDGE = "tun-int";
    public static final String OS_TO_K8S_INTEGRATION_BRIDGE = "os-k8s-int";
    public static final String K8S_INTEGRATION_TO_OS_BRIDGE = "k8s-int-os";
    public static final String OS_TO_K8S_EXTERNAL_BRIDGE = "os-k8s-ext";
    public static final String K8S_EXTERNAL_TO_OS_BRIDGE = "k8s-ext-os";
    public static final String VXLAN_TUNNEL = "vxlan";
    public static final String GRE_TUNNEL = "gre";
    public static final String GENEVE_TUNNEL = "geneve";

    public static final String VXLAN = "vxlan";
    public static final String GRE = "gre";
    public static final String GENEVE = "geneve";

    public static final String DEFAULT_CLUSTER_NAME = "default";
    public static final String DEFAULT_CONFIG_MODE = "NORMAL";

    public static final int DEFAULT_SEGMENT_ID = 100;
    public static final String DEFAULT_EXTERNAL_GATEWAY_MAC = "fa:00:00:00:00:01";
    public static final String DEFAULT_EXTERNAL_BRIDGE_MAC = "fa:00:00:00:00:02";
    public static final String DEFAULT_INTG_BRIDGE_MAC = "fa:00:00:00:00:03";
}
