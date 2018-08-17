/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.domain.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.DomainIntentConfigurable;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.domain.DomainIntent;
import org.onosproject.net.domain.DomainIntentOperation;
import org.onosproject.net.domain.DomainIntentOperations;
import org.onosproject.net.domain.DomainIntentService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.domain.DomainIntentOperation.Type.ADD;
import static org.onosproject.net.domain.DomainIntentOperation.Type.REMOVE;

/**
 * {@link DomainIntentService} implementation class.
 */
@Component(immediate = true, service = DomainIntentService.class)
public class DomainIntentManager implements DomainIntentService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    private ExecutorService executorService =
            newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                    groupedThreads("onos/domain-intent-mgmt", "%d", log));

    private final Map<DeviceId, DriverHandler> driverHandlers = Maps.newConcurrentMap();
    private final Map<DeviceId, DomainIntentConfigurable> driversMap = Maps.newConcurrentMap();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void sumbit(DomainIntentOperations domainOperations) {
        executorService.execute(new DomainIntentProcessor(domainOperations));
    }

    private class DomainIntentProcessor implements Runnable {
        private final DomainIntentOperations idos;

        private final List<DomainIntentOperation> stages;
        private boolean hasFailed = false;

        public DomainIntentProcessor(DomainIntentOperations dios) {
            this.idos = checkNotNull(dios);
            this.stages = Lists.newArrayList(checkNotNull(dios.stages()));
        }

        @Override
        public synchronized void run() {
            if (stages.size() > 0) {
                process(stages.remove(0));
            } else if (!hasFailed) {
                idos.callback().onSuccess(idos);
            }
        }


        private void process(DomainIntentOperation dio) {
            Optional<DomainIntentConfigurable> config =
                    dio.intent().filteredIngressPoints().stream()
                            .map(x -> getDomainIntentConfigurable(x.connectPoint().deviceId()))
                            .filter(Objects::nonNull)
                            .findAny();
            DomainIntent domainIntent = null;
            if (config.isPresent()) {
                if (dio.type() == ADD) {
                    domainIntent = config.get().sumbit(dio.intent());
                } else if (dio.type() == REMOVE) {
                    domainIntent = config.get().remove(dio.intent());
                }
                executorService.execute(this);
            } else {
                log.error("Ingresses devices does not support " +
                        "DomainIntentConfigurable. Installation failed");
                hasFailed = true;
            }
            if (domainIntent == null) {
                log.error("Installation failed for Domain Intent {}", dio.intent());
                hasFailed = true;
            }
            if (hasFailed) {
                DomainIntentOperations failedBuilder = DomainIntentOperations.builder()
                        .add(dio.intent()).build();
                idos.callback().onError(failedBuilder);
                executorService.execute(this);
            }
        }

    }

    private DomainIntentConfigurable getDomainIntentConfigurable(DeviceId deviceId) {
        return driversMap.computeIfAbsent(deviceId, this::initDomainIntentDriver);
    }

    private DomainIntentConfigurable initDomainIntentDriver(DeviceId deviceId) {

        // Attempt to lookup the handler in the cache
        DriverHandler handler = driverHandlers.get(deviceId);
        if (handler == null) {
            try {
                // Otherwise create it and if it has DomainIntentConfig behaviour, cache it
                handler = driverService.createHandler(deviceId);

                if (!handler.driver().hasBehaviour(DomainIntentConfigurable.class)) {
                    log.warn("DomainIntentConfig behaviour not supported for device {}",
                            deviceId);
                    return null;
                }
            } catch (ItemNotFoundException e) {
                log.warn("No applicable driver for device {}", deviceId);
                return null;
            }
            driverHandlers.put(deviceId, handler);
        }
        // Always (re)initialize the pipeline behaviour
        log.info("Driver {} bound to device {} ... initializing driver",
                handler.driver().name(), deviceId);

        return handler.behaviour(DomainIntentConfigurable.class);
    }
}
