package org.onlab.onos.store.flow;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onlab.onos.event.AbstractEvent;
import org.onlab.onos.net.DeviceId;

/**
 * Describes a device replicainfo event.
 */
public class ReplicaInfoEvent extends AbstractEvent<ReplicaInfoEvent.Type, DeviceId> {

    private final ReplicaInfo replicaInfo;

    /**
     * Types of Replica info event.
     */
    public enum Type {
        /**
         * Event to notify that master placement should be changed.
         */
        MASTER_CHANGED,
        //
        // BACKUPS_CHANGED?
    }


    /**
     * Creates an event of a given type and for the specified device,
     * and replica info.
     *
     * @param type        replicainfo event type
     * @param device      event device subject
     * @param replicaInfo replicainfo
     */
    public ReplicaInfoEvent(Type type, DeviceId device, ReplicaInfo replicaInfo) {
        super(type, device);
        this.replicaInfo = checkNotNull(replicaInfo);
    }

    /**
     * Returns the current replica information for the subject.
     *
     * @return replica information for the subject
     */
    public ReplicaInfo replicaInfo() {
        return replicaInfo;
    };
}
