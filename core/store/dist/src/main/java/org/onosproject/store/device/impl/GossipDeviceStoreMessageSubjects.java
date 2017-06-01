/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.store.device.impl;

import org.onosproject.store.cluster.messaging.MessageSubject;

/**
 * MessageSubjects used by GossipDeviceStore peer-peer communication.
 */
public final class GossipDeviceStoreMessageSubjects {

    private GossipDeviceStoreMessageSubjects() {}

    public static final MessageSubject DEVICE_UPDATE = new MessageSubject("peer-device-update");
    public static final MessageSubject DEVICE_OFFLINE = new MessageSubject("peer-device-offline");
    public static final MessageSubject DEVICE_REMOVE_REQ = new MessageSubject("peer-device-remove-request");
    public static final MessageSubject DEVICE_REMOVED = new MessageSubject("peer-device-removed");
    public static final MessageSubject PORT_UPDATE = new MessageSubject("peer-port-update");
    public static final MessageSubject PORT_STATUS_UPDATE = new MessageSubject("peer-port-status-update");

    public static final MessageSubject DEVICE_ADVERTISE = new MessageSubject("peer-device-advertisements");
    // to be used with 3-way anti-entropy process
    public static final MessageSubject DEVICE_REQUEST = new MessageSubject("peer-device-request");

}
