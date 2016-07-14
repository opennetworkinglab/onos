/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.utils;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.common.collect.Maps;
import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsService;
import org.onlab.osgi.DefaultServiceDirectory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Agent that implements usage and performance monitoring via the metrics service.
 */
public class MeteringAgent {

    private Counter exceptionCounter;
    private Counter perObjExceptionCounter;
    private MetricsService metricsService;
    private MetricsComponent metricsComponent;
    private MetricsFeature metricsFeature;
    private final Map<String, Timer> perObjOpTimers = Maps.newConcurrentMap();
    private final Map<String, Timer> perOpTimers = Maps.newConcurrentMap();
    private Timer perPrimitiveTimer;
    private Timer perObjTimer;
    private MetricsFeature wildcard;
    private final boolean activated;
    private Context nullTimer;

    /**
     * Constructs a new MeteringAgent for a given distributed primitive.
     * Instantiates the metrics service
     * Initializes all the general metrics for that object
     *
     * @param primitiveName Type of primitive to be metered
     * @param objName Global name of the primitive
     * @param activated boolean flag for whether metering is enabled or not
     */
    public MeteringAgent(String primitiveName, String objName, boolean activated) {
        checkNotNull(objName, "Object name cannot be null");
        this.activated = activated;
        nullTimer = new Context(null, "");
        if (this.activated) {
            this.metricsService = DefaultServiceDirectory.getService(MetricsService.class);
            this.metricsComponent = metricsService.registerComponent(primitiveName);
            this.metricsFeature = metricsComponent.registerFeature(objName);
            this.wildcard = metricsComponent.registerFeature("*");
            this.perObjTimer = metricsService.createTimer(metricsComponent, metricsFeature, "*");
            this.perPrimitiveTimer = metricsService.createTimer(metricsComponent, wildcard, "*");
            this.perObjExceptionCounter = metricsService.createCounter(metricsComponent, metricsFeature, "exceptions");
            this.exceptionCounter = metricsService.createCounter(metricsComponent, wildcard, "exceptions");
        }
    }

    /**
     * Initializes a specific timer for a given operation.
     *
     * @param op Specific operation being metered
     * @return timer context
     */
    public Context startTimer(String op) {
        if (!activated) {
            return nullTimer;
        }
        // Check if timer exists, if it doesn't creates it
        final Timer currTimer = perObjOpTimers.computeIfAbsent(op, timer ->
                metricsService.createTimer(metricsComponent, metricsFeature, op));
        perOpTimers.computeIfAbsent(op, timer -> metricsService.createTimer(metricsComponent, wildcard, op));
        // Starts timer
        return new Context(currTimer.time(), op);
    }

    /**
     * Timer.Context with a specific operation.
     */
    public class Context {
        private final Timer.Context context;
        private final String operation;

        /**
         * Constructs Context.
         *
         * @param context context
         * @param operation operation name
         */
        public Context(Timer.Context context, String operation) {
            this.context = context;
            this.operation = operation;
        }

        /**
         * Stops timer given a specific context and updates all related metrics.
         * @param e throwable
         */
        public void stop(Throwable e) {
            if (!activated) {
                return;
            }
            if (e == null) {
                //Stop and updates timer with specific measurements per map, per operation
                final long time = context.stop();
                //updates timer with aggregated measurements per map
                perOpTimers.get(operation).update(time, TimeUnit.NANOSECONDS);
                //updates timer with aggregated measurements per map
                perObjTimer.update(time, TimeUnit.NANOSECONDS);
                //updates timer with aggregated measurements per all Consistent Maps
                perPrimitiveTimer.update(time, TimeUnit.NANOSECONDS);
            } else {
                exceptionCounter.inc();
                perObjExceptionCounter.inc();
            }
        }
    }

}
