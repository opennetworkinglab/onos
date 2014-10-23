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
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostListener;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.topology.TopologyEvent;
import org.onlab.onos.net.topology.TopologyListener;
import org.onlab.onos.net.topology.TopologyService;
import org.slf4j.Logger;

/**
 * ONOS Topology Metrics Application that collects topology-related metrics.
 */
@Component(immediate = true)
@Service
public class TopologyMetrics implements TopologyMetricsService {
    private static final Logger log = getLogger(TopologyMetrics.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    private LinkedList<Event> lastEvents = new LinkedList<>();
    private static final int LAST_EVENTS_MAX_N = 100;

    private final DeviceListener deviceListener = new InnerDeviceListener();
    private final HostListener hostListener = new InnerHostListener();
    private final LinkListener linkListener = new InnerLinkListener();
    private final TopologyListener topologyListener =
        new InnerTopologyListener();

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

        // Register for all topology-related events
        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);
        linkService.addListener(linkListener);
        topologyService.addListener(topologyListener);

        log.info("ONOS Topology Metrics started.");
    }

    @Deactivate
    public void deactivate() {
        // De-register from all topology-related events
        deviceService.removeListener(deviceListener);
        hostService.removeListener(hostListener);
        linkService.removeListener(linkListener);
        topologyService.removeListener(topologyListener);

        removeMetrics();
        clear();
        log.info("ONOS Topology Metrics stopped.");
    }

    @Override
    public List<Event> getEvents() {
        synchronized (lastEvents) {
            return ImmutableList.<Event>copyOf(lastEvents);
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

    /**
     * Records an event.
     *
     * @param event the event to record
     * @param updateEventRateMeter if true, update the Event Rate Meter
     */
    private void recordEvent(Event event, boolean updateEventRateMeter) {
        synchronized (lastEvents) {
            lastEventTimestampEpochMs = System.currentTimeMillis();
            if (updateEventRateMeter) {
                eventRateMeter.mark(1);
            }

            //
            // Keep only the last N events, where N = LAST_EVENTS_MAX_N
            //
            while (lastEvents.size() >= LAST_EVENTS_MAX_N) {
                lastEvents.remove();
            }
            lastEvents.add(event);
        }
    }

    /**
     * Inner Device Event Listener class.
     */
    private class InnerDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            recordEvent(event, true);
            log.debug("Device Event: time = {} type = {} event = {}",
                      event.time(), event.type(), event);
        }
    }

    /**
     * Inner Host Event Listener class.
     */
    private class InnerHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            recordEvent(event, true);
            log.debug("Host Event: time = {} type = {} event = {}",
                      event.time(), event.type(), event);
        }
    }

    /**
     * Inner Link Event Listener class.
     */
    private class InnerLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            recordEvent(event, true);
            log.debug("Link Event: time = {} type = {} event = {}",
                      event.time(), event.type(), event);
        }
    }

    /**
     * Inner Topology Event Listener class.
     */
    private class InnerTopologyListener implements TopologyListener {
        @Override
        public void event(TopologyEvent event) {
            //
            // NOTE: Don't update the eventRateMeter, because the real
            // events are already captured/counted.
            //
            recordEvent(event, false);
            log.debug("Topology Event: time = {} type = {} event = {}",
                      event.time(), event.type(), event);
            for (Event reason : event.reasons()) {
                log.debug("Topology Event Reason: time = {} type = {} event = {}",
                          reason.time(), reason.type(), reason);
            }
        }
    }

    /**
     * Clears the internal state.
     */
    private void clear() {
        synchronized (lastEvents) {
            lastEventTimestampEpochMs = 0;
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
