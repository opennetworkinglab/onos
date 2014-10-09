package org.onlab.onos.store.device.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceClockProviderService;
import org.onlab.onos.net.device.DeviceClockService;
import org.onlab.onos.net.device.DeviceMastershipTerm;
import org.onlab.onos.net.device.Timestamp;
import org.onlab.onos.store.common.impl.DeviceMastershipBasedTimestamp;
import org.slf4j.Logger;

/**
 * Clock service to issue Timestamp based on Device Mastership.
 */
@Component(immediate = true)
@Service
public class DeviceClockManager implements DeviceClockService, DeviceClockProviderService {

    private final Logger log = getLogger(getClass());

    // TODO: Implement per device ticker that is reset to 0 at the beginning of a new term.
    private final AtomicInteger ticker = new AtomicInteger(0);
    private ConcurrentMap<DeviceId, DeviceMastershipTerm> deviceMastershipTerms = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Timestamp getTimestamp(DeviceId deviceId) {
        DeviceMastershipTerm term = deviceMastershipTerms.get(deviceId);
        if (term == null) {
            throw new IllegalStateException("Requesting timestamp for a deviceId without mastership");
        }
        return new DeviceMastershipBasedTimestamp(term.termNumber(), ticker.incrementAndGet());
    }

    @Override
    public void setDeviceMastershipTerm(DeviceId deviceId, DeviceMastershipTerm term) {
        deviceMastershipTerms.put(deviceId, term);
    }
}
