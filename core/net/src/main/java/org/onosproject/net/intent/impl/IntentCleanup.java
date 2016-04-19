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
package org.onosproject.net.intent.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.Key;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This component cleans up intents that have encountered errors or otherwise
 * stalled during installation or withdrawal.
 * <p>
 * It periodically polls (based on configured period) for pending and CORRUPT
 * intents from the store and retries. It also listens for CORRUPT event
 * notifications, which signify errors in processing, and retries.
 * </p>
 */
@Component(immediate = true)
public class IntentCleanup implements Runnable, IntentListener {

    private static final Logger log = getLogger(IntentCleanup.class);

    private static final int DEFAULT_PERIOD = 5; //seconds
    private static final int DEFAULT_THRESHOLD = 5; //tries

    @Property(name = "enabled", boolValue = true,
              label = "Enables/disables the intent cleanup component")
    private boolean enabled = true;

    @Property(name = "period", intValue = DEFAULT_PERIOD,
              label = "Frequency in ms between cleanup runs")
    protected int period = DEFAULT_PERIOD;
    private long periodMs;

    @Property(name = "retryThreshold", intValue = DEFAULT_THRESHOLD,
            label = "Number of times to retry CORRUPT intent without delay")
    protected int retryThreshold = DEFAULT_THRESHOLD;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService service;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private ExecutorService executor;
    private Timer timer;
    private TimerTask timerTask;

    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());
        executor = newSingleThreadExecutor(groupedThreads("onos/intent", "cleanup", log));
        timer = new Timer("onos-intent-cleanup-timer");
        service.addListener(this);
        adjustRate();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        service.removeListener(this);
        timer.cancel();
        timerTask = null;
        executor.shutdown();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();

        int newPeriod;
        boolean newEnabled;
        try {
            String s = get(properties, "period");
            newPeriod = isNullOrEmpty(s) ? period : Integer.parseInt(s.trim());

            s = get(properties, "retryThreshold");
            retryThreshold = isNullOrEmpty(s) ? retryThreshold : Integer.parseInt(s.trim());

            s = get(properties, "enabled");
            newEnabled = isNullOrEmpty(s) ? enabled : Boolean.parseBoolean(s.trim());
        } catch (NumberFormatException e) {
            log.warn(e.getMessage());
            newPeriod = period;
            newEnabled = enabled;
        }

        // Any change in the following parameters implies hard restart
        if (newPeriod != period || enabled != newEnabled) {
            period = newPeriod;
            enabled = newEnabled;
            adjustRate();
        }

        log.info("Settings: enabled={}, period={}, retryThreshold={}",
                 enabled, period, retryThreshold);
    }

    protected void adjustRate() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        if (enabled) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    executor.execute(IntentCleanup.this);
                }
            };

            periodMs = period * 1_000; //convert to ms
            timer.scheduleAtFixedRate(timerTask, periodMs, periodMs);
        }
    }


    @Override
    public void run() {
        try {
            cleanup();
        } catch (Exception e) {
            log.warn("Caught exception during Intent cleanup", e);
        }
    }

    private void resubmitCorrupt(IntentData intentData, boolean checkThreshold) {
        if (checkThreshold && intentData.errorCount() >= retryThreshold) {
            return; // threshold met or exceeded
        }

        switch (intentData.request()) {
            case INSTALL_REQ:
                service.submit(intentData.intent());
                break;
            case WITHDRAW_REQ:
                service.withdraw(intentData.intent());
                break;
            default:
                log.warn("Trying to resubmit corrupt/failed intent {} in state {} with request {}",
                         intentData.key(), intentData.state(), intentData.request());
                break;
        }
    }

    private void resubmitPendingRequest(IntentData intentData) {
        switch (intentData.request()) {
            case INSTALL_REQ:
                service.submit(intentData.intent());
                break;
            case WITHDRAW_REQ:
                service.withdraw(intentData.intent());
                break;
            case PURGE_REQ:
                service.purge(intentData.intent());
                break;
            default:
                log.warn("Failed to resubmit pending intent {} in state {} with request {}",
                         intentData.key(), intentData.state(), intentData.request());
                break;
        }
    }

    /**
     * Iterates through corrupt, failed and pending intents and
     * re-submit/withdraw appropriately.
     */
    private void cleanup() {
        int corruptCount = 0, failedCount = 0, stuckCount = 0, pendingCount = 0;

        for (IntentData intentData : store.getIntentData(true, periodMs)) {
            switch (intentData.state()) {
                case FAILED:
                    resubmitCorrupt(intentData, false);
                    failedCount++;
                    break;
                case CORRUPT:
                    resubmitCorrupt(intentData, false);
                    corruptCount++;
                    break;
                case INSTALLING: //FALLTHROUGH
                case WITHDRAWING:
                    resubmitPendingRequest(intentData);
                    stuckCount++;
                    break;
                default:
                    //NOOP
                    break;
            }
        }

        for (IntentData intentData : store.getPendingData(true, periodMs)) {
            resubmitPendingRequest(intentData);
            stuckCount++;
        }

        if (corruptCount + failedCount + stuckCount + pendingCount > 0) {
            log.debug("Intent cleanup ran and resubmitted {} corrupt, {} failed, {} stuck, and {} pending intents",
                    corruptCount, failedCount, stuckCount, pendingCount);
        }
    }

    @Override
    public void event(IntentEvent event) {
        // this is the fast path for CORRUPT intents, retry on event notification.
        //TODO we might consider using the timer to back off for subsequent retries
        if (enabled && event.type() == IntentEvent.Type.CORRUPT) {
            Key key = event.subject().key();
            if (store.isMaster(key)) {
                IntentData data = store.getIntentData(event.subject().key());
                resubmitCorrupt(data, true);
            }
        }
    }
}
