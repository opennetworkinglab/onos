/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtn.manager;

import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.vtnrsc.event.VtnRscEventFeedback;

/**
 * VTN application that applies configuration and flows to the device.
 */
public interface VtnService {

    /**
     * Creates a vxlan tunnel and creates the ovs when a ovs controller node is
     * detected.
     *
     * @param device controller-type device
     */
    void onControllerDetected(Device device);

    /**
     * Drops a vxlan tunnel and drops the ovs when a ovs controller node is
     * vanished.
     *
     * @param device controller-type device
     */
    void onControllerVanished(Device device);

    /**
     * Applies default forwarding flows when a ovs is detected.
     *
     * @param device switch-type device
     */
    void onOvsDetected(Device device);

    /**
     * Remove default forwarding flows when a ovs is vanished.
     *
     * @param device switch-type device
     */
    void onOvsVanished(Device device);

    /**
     * Applies multicast flows and tunnel flows when a VM is detected.
     *
     * @param host a VM
     */
    void onHostDetected(Host host);

    /**
     * Remove multicast flows and tunnel flows when a VM is vanished.
     *
     * @param host a VM
     */
    void onHostVanished(Host host);

    /**
     * Applies east west flows when neutron created router interface.
     *
     * @param l3Feedback VtnrscEventFeedback
     */
    void onRouterInterfaceDetected(VtnRscEventFeedback l3Feedback);

    /**
     * Remove east west flows when neutron removed router interface.
     *
     * @param l3Feedback VtnrscEventFeedback
     */
    void onRouterInterfaceVanished(VtnRscEventFeedback l3Feedback);

    /**
     * Applies north south flows when neutron bind floating ip.
     *
     * @param l3Feedback VtnrscEventFeedback
     */
    void onFloatingIpDetected(VtnRscEventFeedback l3Feedback);

    /**
     * Applies north south flows when neutron unbind floating ip.
     *
     * @param l3Feedback VtnrscEventFeedback
     */
    void onFloatingIpVanished(VtnRscEventFeedback l3Feedback);

}
