package org.onosproject.segmentrouting;

import org.onosproject.net.DeviceId;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Segment Routing Flow Objective Context.
 */
public class SRObjectiveContext implements ObjectiveContext {
    enum ObjectiveType {
        FILTER,
        FORWARDING
    }
    private final DeviceId deviceId;
    private final ObjectiveType type;

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingManager.class);

    SRObjectiveContext(DeviceId deviceId, ObjectiveType type) {
        this.deviceId = deviceId;
        this.type = type;
    }
    @Override
    public void onSuccess(Objective objective) {
        log.debug("{} objective operation successful in device {}",
                type.name(), deviceId);
    }

    @Override
    public void onError(Objective objective, ObjectiveError error) {
        log.warn("{} objective {} operation failed with error: {} in device {}",
                type.name(), objective, error, deviceId);
    }
}

