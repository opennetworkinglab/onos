package org.onlab.onos.metrics.intent;

import java.util.List;
import org.onlab.metrics.EventMetric;
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
     * Gets the Event Metric for the intent SUBMITTED events.
     *
     * @return the Event Metric for the intent SUBMITTED events.
     */
    public EventMetric intentSubmittedEventMetric();

    /**
     * Gets the Event Metric for the intent INSTALLED events.
     *
     * @return the Event Metric for the intent INSTALLED events.
     */
    public EventMetric intentInstalledEventMetric();

    /**
     * Gets the Event Metric for the intent WITHDRAW_REQUESTED events.
     *
     * TODO: This intent event is not implemented yet.
     *
     * @return the Event Metric for the intent WITHDRAW_REQUESTED events.
     */
    public EventMetric intentWithdrawRequestedEventMetric();

    /**
     * Gets the Event Metric for the intent WITHDRAWN events.
     *
     * @return the Event Metric for the intent WITHDRAWN events.
     */
    public EventMetric intentWithdrawnEventMetric();
}
