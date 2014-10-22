package org.onlab.onos.metrics.topology;

import java.util.List;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import org.onlab.onos.net.topology.TopologyEvent;

/**
 * Service interface exported by TopologyMetrics.
 */
public interface TopologyMetricsService {
    /**
     * Gets the last saved topology events.
     *
     * @return the last saved topology events.
     */
    public List<TopologyEvent> getEvents();

    /**
     * Gets the Metrics' Gauge for the last topology event timestamp
     * (ms from the epoch).
     *
     * @return the Metrics' Gauge for the last topology event timestamp
     * (ms from the epoch)
     */
    public Gauge<Long> lastEventTimestampEpochMsGauge();

    /**
     * Gets the Metrics' Meter for the topology events rate.
     *
     * @return the Metrics' Meter for the topology events rate
     */
    public Meter eventRateMeter();
}
