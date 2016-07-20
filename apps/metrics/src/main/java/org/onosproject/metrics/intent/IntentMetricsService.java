/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.metrics.intent;

import java.util.List;
import org.onlab.metrics.EventMetric;
import org.onosproject.net.intent.IntentEvent;

/**
 * Service interface exported by IntentMetrics.
 */
public interface IntentMetricsService {
    /**
     * Gets the last saved intent events.
     *
     * @return the last saved intent events
     */
    List<IntentEvent> getEvents();

    /**
     * Gets the Event Metric for the intent INSTALL_REQ events.
     *
     * @return the Event Metric for the intent INSTALL_REQ events
     */
    EventMetric intentSubmittedEventMetric();

    /**
     * Gets the Event Metric for the intent INSTALLED events.
     *
     * @return the Event Metric for the intent INSTALLED events
     */
    EventMetric intentInstalledEventMetric();

    /**
     * Gets the Event Metric for the intent FAILED events.
     *
     * @return the Event Metric for the intent FAILED events
     */
    EventMetric intentFailedEventMetric();

    /**
     * Gets the Event Metric for the intent WITHDRAW_REQ events.
     *
     * @return the Event Metric for the intent WITHDRAW_REQ events
     */
    EventMetric intentWithdrawRequestedEventMetric();

    /**
     * Gets the Event Metric for the intent WITHDRAWN events.
     *
     * @return the Event Metric for the intent WITHDRAWN events
     */
    EventMetric intentWithdrawnEventMetric();

    /**
     * Gets the Event Metric for the intent PURGED events.
     *
     * @return the Event Metric for the intent PURGED events
     */
    EventMetric intentPurgedEventMetric();
}
