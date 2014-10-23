package org.onlab.onos.metrics.intent;

import java.util.List;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import org.onlab.onos.net.intent.IntentEvent;

/**
 * Service interface exported by IntentMetrics.
 */
public interface IntentMetricsService {
    /**
     * Gets the last saved intent events.
     *
     * @return the last saved intent events.
     */
    public List<IntentEvent> getEvents();

    /**
     * Gets the Metrics' Gauge for the intent SUBMITTED event timestamp
     * (ms from the epoch).
     *
     * @return the Metrics' Gauge for the intent SUBMITTED event timestamp
     * (ms from the epoch)
     */
    public Gauge<Long> intentSubmittedTimestampEpochMsGauge();

    /**
     * Gets the Metrics' Gauge for the intent INSTALLED event timestamp
     * (ms from the epoch).
     *
     * @return the Metrics' Gauge for the intent INSTALLED event timestamp
     * (ms from the epoch)
     */
    public Gauge<Long> intentInstalledTimestampEpochMsGauge();

    /**
     * Gets the Metrics' Gauge for the intent WITHDRAW_REQUESTED event
     * timestamp (ms from the epoch).
     *
     * TODO: This intent event is not implemented yet.
     *
     * @return the Metrics' Gauge for the intent WITHDRAW_REQUESTED event
     * timestamp (ms from the epoch)
     */
    public Gauge<Long> intentWithdrawRequestedTimestampEpochMsGauge();

    /**
     * Gets the Metrics' Gauge for the intent WITHDRAWN event timestamp
     * (ms from the epoch).
     *
     * @return the Metrics' Gauge for the intent WITHDRAWN event timestamp
     * (ms from the epoch)
     */
    public Gauge<Long> intentWithdrawnTimestampEpochMsGauge();

    /**
     * Gets the Metrics' Meter for the submitted intents event rate.
     *
     * @return the Metrics' Meter for the submitted intents event rate
     */
    public Meter intentSubmittedRateMeter();

    /**
     * Gets the Metrics' Meter for the installed intents event rate.
     *
     * @return the Metrics' Meter for the installed intent event rate
     */
    public Meter intentInstalledRateMeter();

    /**
     * Gets the Metrics' Meter for the withdraw requested intents event rate.
     *
     * @return the Metrics' Meter for the withdraw requested intents event rate
     */
    public Meter intentWithdrawRequestedRateMeter();

    /**
     * Gets the Metrics' Meter for the withdraw completed intents event rate.
     *
     * @return the Metrics' Meter for the withdraw completed intents event rate
     */
    public Meter intentWithdrawnRateMeter();
}
