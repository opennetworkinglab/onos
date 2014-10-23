package org.onlab.onos.metrics.intent;

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
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentListener;
import org.onlab.onos.net.intent.IntentService;
import org.slf4j.Logger;

/**
 * ONOS Intent Metrics Application that collects intent-related metrics.
 */
@Component(immediate = true)
@Service
public class IntentMetrics implements IntentMetricsService,
                                      IntentListener {
    private static final Logger log = getLogger(IntentMetrics.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;
    private LinkedList<IntentEvent> lastEvents = new LinkedList<>();
    private static final int LAST_EVENTS_MAX_N = 100;

    //
    // Metrics
    //
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MetricsService metricsService;
    //
    private static final String COMPONENT_NAME = "Intent";
    private static final String FEATURE_SUBMITTED_NAME = "Submitted";
    private static final String FEATURE_INSTALLED_NAME = "Installed";
    private static final String FEATURE_WITHDRAW_REQUESTED_NAME =
        "WithdrawRequested";
    private static final String FEATURE_WITHDRAWN_NAME = "Withdrawn";
    private static final String GAUGE_TIMESTAMP_NAME = "Timestamp.EpochMs";
    private static final String METER_RATE_NAME = "Rate";
    //
    private MetricsComponent metricsComponent;
    private MetricsFeature metricsFeatureSubmitted;
    private MetricsFeature metricsFeatureInstalled;
    private MetricsFeature metricsFeatureWithdrawRequested;
    private MetricsFeature metricsFeatureWithdrawn;
    //
    // Timestamps:
    //  - Intent Submitted API operation (ms from the Epoch)
    //  - Intent Installed operation completion (ms from the Epoch)
    //  - Intent Withdraw Requested API operation (ms from the Epoch)
    //  - Intent Withdrawn operation completion (ms from the Epoch)
    //
    private volatile long intentSubmittedTimestampEpochMs = 0;
    private volatile long intentInstalledTimestampEpochMs = 0;
    private volatile long intentWithdrawRequestedTimestampEpochMs = 0;
    private volatile long intentWithdrawnTimestampEpochMs = 0;
    //
    private Gauge<Long> intentSubmittedTimestampEpochMsGauge;
    private Gauge<Long> intentInstalledTimestampEpochMsGauge;
    private Gauge<Long> intentWithdrawRequestedTimestampEpochMsGauge;
    private Gauge<Long> intentWithdrawnTimestampEpochMsGauge;
    //
    // Rate meters:
    //  - Rate of the Submitted Intent API operations
    //  - Rate of the Installed Intent operations
    //  - Rate of the Withdrawn Requested Intent API operations
    //  - Rate of the Withdrawn Intent operations
    //
    private Meter intentSubmittedRateMeter;
    private Meter intentInstalledRateMeter;
    private Meter intentWithdrawRequestedRateMeter;
    private Meter intentWithdrawnRateMeter;

    @Activate
    protected void activate() {
        clear();
        registerMetrics();
        intentService.addListener(this);
        log.info("ONOS Intent Metrics started.");
    }

    @Deactivate
    public void deactivate() {
        intentService.removeListener(this);
        removeMetrics();
        clear();
        log.info("ONOS Intent Metrics stopped.");
    }

    @Override
    public List<IntentEvent> getEvents() {
        synchronized (lastEvents) {
            return ImmutableList.<IntentEvent>copyOf(lastEvents);
        }
    }

    @Override
    public Gauge<Long> intentSubmittedTimestampEpochMsGauge() {
        return intentSubmittedTimestampEpochMsGauge;
    }

    @Override
    public Gauge<Long> intentInstalledTimestampEpochMsGauge() {
        return intentInstalledTimestampEpochMsGauge;
    }

    @Override
    public Gauge<Long> intentWithdrawRequestedTimestampEpochMsGauge() {
        return intentWithdrawRequestedTimestampEpochMsGauge;
    }

    @Override
    public Gauge<Long> intentWithdrawnTimestampEpochMsGauge() {
        return intentWithdrawnTimestampEpochMsGauge;
    }

    @Override
    public Meter intentSubmittedRateMeter() {
        return intentSubmittedRateMeter;
    }

    @Override
    public Meter intentInstalledRateMeter() {
        return intentInstalledRateMeter;
    }

    @Override
    public Meter intentWithdrawRequestedRateMeter() {
        return intentWithdrawRequestedRateMeter;
    }

    @Override
    public Meter intentWithdrawnRateMeter() {
        return intentWithdrawnRateMeter;
    }

    @Override
    public void event(IntentEvent event) {
        synchronized (lastEvents) {
            //
            // TODO: The processing below is incomplete: we don't have
            // an event equivalent of "Withdraw Requested"
            //
            switch (event.type()) {
            case SUBMITTED:
                intentSubmittedTimestampEpochMs = System.currentTimeMillis();
                intentSubmittedRateMeter.mark(1);
                break;
            case INSTALLED:
                intentInstalledTimestampEpochMs = System.currentTimeMillis();
                intentInstalledRateMeter.mark(1);
                break;
            case FAILED:
                // TODO: Just ignore?
                break;
                /*
            case WITHDRAW_REQUESTED:
                intentWithdrawRequestedTimestampEpochMs =
                    System.currentTimeMillis();
                    intentWithdrawRequestedRateMeter.mark(1);
                break;
                */
            case WITHDRAWN:
                intentWithdrawnTimestampEpochMs = System.currentTimeMillis();
                intentWithdrawnRateMeter.mark(1);
                break;
            default:
                break;
            }

            //
            // Keep only the last N events, where N = LAST_EVENTS_MAX_N
            //
            while (lastEvents.size() >= LAST_EVENTS_MAX_N) {
                lastEvents.remove();
            }
            lastEvents.add(event);
        }

        log.debug("Intent Event: time = {} type = {} event = {}",
                  event.time(), event.type(), event);
    }

    /**
     * Clears the internal state.
     */
    private void clear() {
        synchronized (lastEvents) {
            intentSubmittedTimestampEpochMs = 0;
            intentInstalledTimestampEpochMs = 0;
            intentWithdrawRequestedTimestampEpochMs = 0;
            intentWithdrawnTimestampEpochMs = 0;
            lastEvents.clear();
        }
    }

    /**
     * Registers the metrics.
     */
    private void registerMetrics() {
        metricsComponent = metricsService.registerComponent(COMPONENT_NAME);
        //
        metricsFeatureSubmitted =
            metricsComponent.registerFeature(FEATURE_SUBMITTED_NAME);
        metricsFeatureInstalled =
            metricsComponent.registerFeature(FEATURE_INSTALLED_NAME);
        metricsFeatureWithdrawRequested =
            metricsComponent.registerFeature(FEATURE_WITHDRAW_REQUESTED_NAME);
        metricsFeatureWithdrawn =
            metricsComponent.registerFeature(FEATURE_WITHDRAWN_NAME);
        //
        intentSubmittedTimestampEpochMsGauge =
            metricsService.registerMetric(metricsComponent,
                                          metricsFeatureSubmitted,
                                          GAUGE_TIMESTAMP_NAME,
                                          new Gauge<Long>() {
                                              @Override
                                              public Long getValue() {
                                                  return intentSubmittedTimestampEpochMs;
                                              }
                                          });
        //
        intentInstalledTimestampEpochMsGauge =
            metricsService.registerMetric(metricsComponent,
                                          metricsFeatureInstalled,
                                          GAUGE_TIMESTAMP_NAME,
                                          new Gauge<Long>() {
                                              @Override
                                              public Long getValue() {
                                                  return intentInstalledTimestampEpochMs;
                                              }
                                          });
        //
        intentWithdrawRequestedTimestampEpochMsGauge =
            metricsService.registerMetric(metricsComponent,
                                          metricsFeatureWithdrawRequested,
                                          GAUGE_TIMESTAMP_NAME,
                                          new Gauge<Long>() {
                                              @Override
                                              public Long getValue() {
                                                  return intentWithdrawRequestedTimestampEpochMs;
                                              }
                                          });
        //
        intentWithdrawnTimestampEpochMsGauge =
            metricsService.registerMetric(metricsComponent,
                                          metricsFeatureWithdrawn,
                                          GAUGE_TIMESTAMP_NAME,
                                          new Gauge<Long>() {
                                              @Override
                                              public Long getValue() {
                                                  return intentWithdrawnTimestampEpochMs;
                                              }
                                          });
        //
        intentSubmittedRateMeter =
            metricsService.createMeter(metricsComponent,
                                       metricsFeatureSubmitted,
                                       METER_RATE_NAME);
        //
        intentInstalledRateMeter =
            metricsService.createMeter(metricsComponent,
                                       metricsFeatureInstalled,
                                       METER_RATE_NAME);
        //
        intentWithdrawRequestedRateMeter =
            metricsService.createMeter(metricsComponent,
                                       metricsFeatureWithdrawRequested,
                                       METER_RATE_NAME);
        //
        intentWithdrawnRateMeter =
            metricsService.createMeter(metricsComponent,
                                       metricsFeatureWithdrawn,
                                       METER_RATE_NAME);
    }

    /**
     * Removes the metrics.
     */
    private void removeMetrics() {
        metricsService.removeMetric(metricsComponent,
                                    metricsFeatureSubmitted,
                                    GAUGE_TIMESTAMP_NAME);
        metricsService.removeMetric(metricsComponent,
                                    metricsFeatureInstalled,
                                    GAUGE_TIMESTAMP_NAME);
        metricsService.removeMetric(metricsComponent,
                                    metricsFeatureWithdrawRequested,
                                    GAUGE_TIMESTAMP_NAME);
        metricsService.removeMetric(metricsComponent,
                                    metricsFeatureWithdrawn,
                                    GAUGE_TIMESTAMP_NAME);
        metricsService.removeMetric(metricsComponent,
                                    metricsFeatureSubmitted,
                                    METER_RATE_NAME);
        metricsService.removeMetric(metricsComponent,
                                    metricsFeatureInstalled,
                                    METER_RATE_NAME);
        metricsService.removeMetric(metricsComponent,
                                    metricsFeatureWithdrawRequested,
                                    METER_RATE_NAME);
        metricsService.removeMetric(metricsComponent,
                                    metricsFeatureWithdrawn,
                                    METER_RATE_NAME);
    }
}
