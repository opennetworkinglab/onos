package org.onlab.onos.metrics.topology;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.LinkedList;
import java.util.List;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsService;
import org.onlab.onos.event.Event;
import org.onlab.onos.net.topology.TopologyEvent;
import org.onlab.onos.net.topology.TopologyListener;
import org.onlab.onos.net.topology.TopologyService;
import org.slf4j.Logger;

/**
 * ONOS Topology Metrics Application that collects topology-related metrics.
 */
@Component(immediate = true)
@Service
public class TopologyMetrics implements TopologyMetricsService,
                                        TopologyListener {
    private static final Logger log = getLogger(TopologyMetrics.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;
    private LinkedList<TopologyEvent> lastEvents = new LinkedList<>();
    private static final int LAST_EVENTS_MAX_N = 10;

    //
    // Metrics
    //
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MetricsService metricsService;
    //
    private static final String COMPONENT_NAME = "Topology";
    private static final String FEATURE_NAME = "EventNotification";
    private static final String GAUGE_NAME = "LastEventTimestamp.EpochMs";
    private static final String METER_NAME = "EventRate";
    //
    private MetricsComponent metricsComponent;
    private MetricsFeature metricsFeatureEventNotification;
    //
    // Timestamp of the last Topology event (ms from the Epoch)
    private volatile long lastEventTimestampEpochMs = 0;
    private Gauge<Long> lastEventTimestampEpochMsGauge;
    // Rate of the Topology events published to the Topology listeners
    private Meter eventRateMeter;

    @Activate
    protected void activate() {
        clear();
        registerMetrics();
        topologyService.addListener(this);
        log.info("ONOS Topology Metrics started.");
    }

    @Deactivate
    public void deactivate() {
        topologyService.removeListener(this);
        removeMetrics();
        clear();
        log.info("ONOS Topology Metrics stopped.");
    }

    @Override
    public List<TopologyEvent> getEvents() {
        synchronized (lastEvents) {
            return ImmutableList.<TopologyEvent>copyOf(lastEvents);
        }
    }

    @Override
    public Gauge<Long> lastEventTimestampEpochMsGauge() {
        return lastEventTimestampEpochMsGauge;
    }

    @Override
    public Meter eventRateMeter() {
        return eventRateMeter;
    }

    @Override
    public void event(TopologyEvent event) {
        lastEventTimestampEpochMs = System.currentTimeMillis();
        //
        // NOTE: If we want to count each "reason" as a separate event,
        // then we should use 'event.reason().size()' instead of '1' to
        // mark the meter below.
        //
        eventRateMeter.mark(1);

        log.debug("Topology Event: time = {} type = {} subject = {}",
                  event.time(), event.type(), event.subject());
        for (Event reason : event.reasons()) {
            log.debug("Topology Event Reason: time = {} type = {} subject = {}",
                      reason.time(), reason.type(), reason.subject());
        }

        //
        // Keep only the last N events, where N = LAST_EVENTS_MAX_N
        //
        synchronized (lastEvents) {
            while (lastEvents.size() >= LAST_EVENTS_MAX_N) {
                lastEvents.remove();
            }
            lastEvents.add(event);
        }
    }

    /**
     * Clears the internal state.
     */
    private void clear() {
        lastEventTimestampEpochMs = 0;
        synchronized (lastEvents) {
            lastEvents.clear();
        }
    }

    /**
     * Registers the metrics.
     */
    private void registerMetrics() {
        metricsComponent = metricsService.registerComponent(COMPONENT_NAME);
        metricsFeatureEventNotification =
            metricsComponent.registerFeature(FEATURE_NAME);
        lastEventTimestampEpochMsGauge =
            metricsService.registerMetric(metricsComponent,
                                          metricsFeatureEventNotification,
                                          GAUGE_NAME,
                                          new Gauge<Long>() {
                                              @Override
                                              public Long getValue() {
                                                  return lastEventTimestampEpochMs;
                                              }
                                          });
        eventRateMeter =
            metricsService.createMeter(metricsComponent,
                                       metricsFeatureEventNotification,
                                       METER_NAME);

    }

    /**
     * Removes the metrics.
     */
    private void removeMetrics() {
        metricsService.removeMetric(metricsComponent,
                                    metricsFeatureEventNotification,
                                    GAUGE_NAME);
        metricsService.removeMetric(metricsComponent,
                                    metricsFeatureEventNotification,
                                    METER_NAME);
    }
}
