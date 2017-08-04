/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.incubator.net.virtual;

import org.onosproject.event.AbstractEvent;

/**
 * Describes virtual network event.
 */
public class VirtualNetworkEvent extends AbstractEvent<VirtualNetworkEvent.Type, NetworkId> {

    /**
     * Type of virtual network events.
     */
    public enum Type {
        /**
         * Signifies that a new tenant identifier was registered.
         */
        TENANT_REGISTERED,
        /**
         * Signifies that a tenant identifier was unregistered.
         */
        TENANT_UNREGISTERED,
        /**
         * Signifies that a new virtual network was added.
         */
        NETWORK_ADDED,
        /**
         * Signifies that a virtual network was updated.
         */
        NETWORK_UPDATED,
        /**
         * Signifies that a virtual network was removed.
         */
        NETWORK_REMOVED,
        /**
         * Signifies that a new virtual network device was added.
         */
        VIRTUAL_DEVICE_ADDED,
        /**
         * Signifies that a virtual network device was updated.
         */
        VIRTUAL_DEVICE_UPDATED,
        /**
         * Signifies that a virtual network device was removed.
         */
        VIRTUAL_DEVICE_REMOVED,
        /**
         * Signifies that a new virtual network port was added.
         */
        VIRTUAL_PORT_ADDED,
        /**
         * Signifies that a virtual network port was updated.
         */
        VIRTUAL_PORT_UPDATED,
        /**
         * Signifies that a virtual network port was removed.
         */
        VIRTUAL_PORT_REMOVED
    }

    private final VirtualDevice virtualDevice;
    private final VirtualPort virtualPort;

    /**
     * Creates an event of a given type and for the specified subject.
     *
     * @param type        event type
     * @param subject     event subject
     */
    public VirtualNetworkEvent(Type type, NetworkId subject) {
        this(type, subject, null, null);
    }

    /**
     * Creates an event of a given type and for the specified subject and the
     * virtual device.
     *
     * @param type          event type
     * @param subject       event subject
     * @param virtualDevice virtual device
     */
    public VirtualNetworkEvent(Type type, NetworkId subject, VirtualDevice virtualDevice) {
        this(type, subject, virtualDevice, null);
    }

    /**
     * Creates an event of a given type and for the specified subject, virtual device and
     * virtual port.
     *
     * @param type          event type
     * @param subject       event subject
     * @param virtualDevice virtual device
     * @param virtualPort   virtual port
     */
    public VirtualNetworkEvent(Type type, NetworkId subject, VirtualDevice virtualDevice,
                                VirtualPort virtualPort) {
        super(type, subject);
        this.virtualDevice = virtualDevice;
        this.virtualPort = virtualPort;
    }

    /**
     * Creates an event of a given type and for the specified subject and time.
     *
     * @param type        event type
     * @param subject     event subject
     * @param time        occurrence time
     */
    public VirtualNetworkEvent(Type type, NetworkId subject, long time) {
        this(type, subject, null, null, time);
    }

    /**
     * Creates an event of a given type and for the specified subject, virtual device and time.
     *
     * @param type          event type
     * @param subject       event subject
     * @param virtualDevice virtual device
     * @param time          occurrence time
     */
    public VirtualNetworkEvent(Type type, NetworkId subject, VirtualDevice virtualDevice,
                               long time) {
        this(type, subject, virtualDevice, null, time);
    }

    /**
     * Creates an event of a given type and for the specified subject, virtual port and time.
     *
     * @param type          device event type
     * @param subject       event subject
     * @param virtualPort   virtual port
     * @param time          occurrence time
     */
    public VirtualNetworkEvent(Type type, NetworkId subject, VirtualPort virtualPort,
                               long time) {
        this(type, subject, null, virtualPort, time);
    }

    /**
     * Creates an event of a given type and for the specified subject, virtual device,
     * virtual port and time.
     *
     * @param type          device event type
     * @param subject       event subject
     * @param virtualDevice virtual device
     * @param virtualPort   virtual port
     * @param time          occurrence time
     */
    private VirtualNetworkEvent(Type type, NetworkId subject, VirtualDevice virtualDevice,
                                VirtualPort virtualPort, long time) {
        super(type, subject, time);
        this.virtualDevice = virtualDevice;
        this.virtualPort = virtualPort;
    }

    /**
     * Returns virtual device affected by event - may be null (for events relating to
     * tenants and virtual networks).
     *
     * @return virtual device
     */
    public VirtualDevice virtualDevice() {
        return virtualDevice;
    }

    /**
     * Returns virtual port affected by event - may be null (for events relating to
     * tenants, virtual networks and virtual devices).
     *
     * @return virtual port
     */
    public VirtualPort virtualPort() {
        return virtualPort;
    }
}
