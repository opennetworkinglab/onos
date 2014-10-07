package org.onlab.onos.store.device.impl;

import org.onlab.onos.store.cluster.messaging.MessageSubject;

/**
 * MessageSubjects used by GossipDeviceStore.
 */
public final class GossipDeviceStoreMessageSubjects {

    private GossipDeviceStoreMessageSubjects() {}

    public static final MessageSubject DEVICE_UPDATE = new MessageSubject("peer-device-update");
    public static final MessageSubject PORT_UPDATE = new MessageSubject("peer-port-update");
    public static final MessageSubject PORT_STATUS_UPDATE = new MessageSubject("peer-port-status-update");
}
