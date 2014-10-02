package org.onlab.onos.store.trivial.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.MastershipTerm;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.store.ClockService;
import org.onlab.onos.store.Timestamp;

//FIXME: Code clone in onos-core-trivial, onos-core-hz-net
/**
 * Dummy implementation of {@link ClockService}.
 */
@Component(immediate = true)
@Service
public class NoOpClockService implements ClockService {

    @Override
    public Timestamp getTimestamp(DeviceId deviceId) {
        return new Timestamp() {

            @Override
            public int compareTo(Timestamp o) {
                throw new IllegalStateException("Never expected to be used.");
            }
        };
    }

    @Override
    public void setMastershipTerm(DeviceId deviceId, MastershipTerm term) {
    }
}
