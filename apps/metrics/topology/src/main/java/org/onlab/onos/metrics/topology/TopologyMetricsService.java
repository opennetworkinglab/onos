package org.onlab.onos.metrics.topology;

import java.util.List;
import org.onlab.metrics.EventMetric;
import org.onlab.onos.event.Event;

/**
 * Service interface exported by TopologyMetrics.
 */
public interface TopologyMetricsService {
    /**
     * Gets the last saved topology events.
     *
     * @return the last saved topology events.
     */
    public List<Event> getEvents();

    /**
     * Gets the Event Metric for the Device Events.
     *
     * @return the Event Metric for the Device Events.
     */
    public EventMetric topologyDeviceEventMetric();

    /**
     * Gets the Event Metric for the Host Events.
     *
     * @return the Event Metric for the Host Events.
     */
    public EventMetric topologyHostEventMetric();

    /**
     * Gets the Event Metric for the Link Events.
     *
     * @return the Event Metric for the Link Events.
     */
    public EventMetric topologyLinkEventMetric();

    /**
     * Gets the Event Metric for the Topology Graph Events.
     *
     * @return the Event Metric for the Topology Graph Events.
     */
    public EventMetric topologyGraphEventMetric();
}
