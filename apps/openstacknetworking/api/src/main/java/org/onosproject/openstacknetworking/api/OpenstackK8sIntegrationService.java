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
package org.onosproject.openstacknetworking.api;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

/**
 * An interface which defines how to integrate openstack with kubernetes.
 */
public interface OpenstackK8sIntegrationService {

    /**
     * Installs K8S pass-through CNI related flow rules.
     *
     * @param k8sNodeIp         kubernetes node IP address
     * @param podCidr           kubernetes POD CIDR
     * @param serviceCidr       kubernetes service CIDR
     * @param podGatewayIp      kubernetes POD's gateway IP
     * @param osK8sIntPortName  openstack k8s integration patch port name
     * @param k8sIntOsPortMac   k8s integration to openstack patch port MAC
     */
    void installCniPtNodeRules(IpAddress k8sNodeIp, IpPrefix podCidr,
                               IpPrefix serviceCidr, IpAddress podGatewayIp,
                               String osK8sIntPortName, MacAddress k8sIntOsPortMac);

    /**
     * Uninstalls K8S pass-through CNI related flow rules.
     *
     * @param k8sNodeIp         kubernetes node IP address
     * @param podCidr           kubernetes POD CIDR
     * @param serviceCidr       kubernetes service CIDR
     * @param podGatewayIp      kubernetes POD's gateway IP
     * @param osK8sIntPortName  openstack k8s integration patch port name
     * @param k8sIntOsPortMac   k8s integration to openstack patch port MAC
     */
    void uninstallCniPtNodeRules(IpAddress k8sNodeIp, IpPrefix podCidr,
                                 IpPrefix serviceCidr, IpAddress podGatewayIp,
                                 String osK8sIntPortName, MacAddress k8sIntOsPortMac);

    /**
     * Installs K8S pass-through CNI related node port flow rules.
     *
     * @param k8sNodeIp         kubernetes node IP address
     * @param osK8sExtPortName  openstack k8s external patch port name
     */
    void installCniPtNodePortRules(IpAddress k8sNodeIp, String osK8sExtPortName);

    /**
     * Uninstalls K8S pass-through CNI related node port flow rules.
     *
     * @param k8sNodeIp         kubernetes node IP address
     * @param osK8sExtPortName  openstack k8s external patch port name
     */
    void uninstallCniPtNodePortRules(IpAddress k8sNodeIp, String osK8sExtPortName);
}
