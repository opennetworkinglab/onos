package org.onlab.onos.store.device.impl;

import org.onlab.onos.store.cluster.messaging.MessageSubject;

// TODO: add prefix to assure uniqueness.
/**
 * MessageSubjects used by GossipDeviceStore peer-peer communication.
 */
public final class GossipDeviceStoreMessageSubjects {

    private GossipDeviceStoreMessageSubjects() {}

    public static final MessageSubject DEVICE_UPDATE = new MessageSubject("peer-device-update");
    public static final MessageSubject DEVICE_OFFLINE = new MessageSubject("peer-device-offline");
    public static final MessageSubject DEVICE_REMOVED = new MessageSubject("peer-device-removed");
    public static final MessageSubject PORT_UPDATE = new MessageSubject("peer-port-update");
    public static final MessageSubject PORT_STATUS_UPDATE = new MessageSubject("peer-port-status-update");

    public static final MessageSubject DEVICE_ADVERTISE = new MessageSubject("peer-device-advertisements");
    // to be used with 3-way anti-entropy process
    public static final MessageSubject DEVICE_REQUEST = new MessageSubject("peer-device-request");
}
