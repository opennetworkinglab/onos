/*
 * Copyright 2015 Open Networking Laboratory
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
import static org.onosproject.net.intent.IntentState.CORRUPT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * FIXME Class to cleanup Intents in CORRUPT state.
 * FIXME move this to its own file eventually (but need executor for now)
 */
@Component(immediate = true)
public class IntentCleanup implements Runnable, IntentListener {

    private static final Logger log = getLogger(IntentManager.class);

    private static final int DEFAULT_PERIOD = 5; //seconds

    @Property(name = "period", intValue = DEFAULT_PERIOD,
              label = "Frequency in ms between cleanup runs")
    protected int period = DEFAULT_PERIOD;

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
        executor = newSingleThreadExecutor(groupedThreads("onos/intent", "cleanup"));
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
        try {
            String s = get(properties, "period");
            newPeriod = isNullOrEmpty(s) ? period : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            log.warn(e.getMessage());
            newPeriod = period;
        }

        // Any change in the following parameters implies hard restart
        if (newPeriod != period) {
            period = newPeriod;
            adjustRate();
        }

        log.info("Settings: period={}", period);
    }

    private void adjustRate() {
        if (timerTask != null) {
            timerTask.cancel();
        }

        timerTask = new TimerTask() {
            @Override
            public void run() {
                executor.submit(IntentCleanup.this);
            }
        };

        long periodMs = period * 1000; //convert to ms
        timer.scheduleAtFixedRate(timerTask, periodMs, periodMs);
    }


    @Override
    public void run() {
        try {
            cleanup();
        } catch (Exception e) {
            log.warn("Caught exception during Intent cleanup", e);
        }
    }

    /**
     * Iterate through CORRUPT intents and re-submit/withdraw.
     *
     * FIXME we want to eventually count number of retries per intent and give up
     * FIXME we probably also want to look at intents that have been stuck
     *       in *_REQ or *ING for "too long".
     */
    private void cleanup() {
        int count = 0;
        for (IntentData intentData : store.getIntentData(true)) {
            if (intentData.state() == CORRUPT) {
                switch (intentData.request()) {
                    case INSTALL_REQ:
                        service.submit(intentData.intent());
                        count++;
                        break;
                    case WITHDRAW_REQ:
                        service.withdraw(intentData.intent());
                        count++;
                        break;
                    default:
                        //TODO this is an error
                        break;
                }
            }
        }
        log.debug("Intent cleanup ran and resubmitted {} intents", count);
    }

    @Override
    public void event(IntentEvent event) {
        if (event.type() == IntentEvent.Type.CORRUPT) {
            // FIXME drop this if we exceed retry threshold
            // just run the whole cleanup script for now
            executor.submit(this);
        }
    }
}
