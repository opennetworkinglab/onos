package org.onlab.onos.core.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;

import org.onlab.metrics.MetricsManager;

/**
 * Metrics service implementation.
 */
@Component(immediate = true)
@Service
public class MetricsManagerComponent extends MetricsManager {

    @Activate
    protected void activate() {
        super.clear();
    }

    @Deactivate
    protected void deactivate() {
        super.clear();
    }
}
