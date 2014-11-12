/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.demo;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.CoreService;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.HostToHostIntent;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentService;
import org.slf4j.Logger;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application to set up demos.
 */
@Component(immediate = true)
@Service
public class DemoInstaller implements DemoAPI {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private ExecutorService worker;

    private ApplicationId appId;

    private final Set<Intent> existingIntents = new HashSet<>();



    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onlab.onos.demo.installer");
        worker = Executors.newFixedThreadPool(1,
                                              new ThreadFactoryBuilder()
                                                      .setNameFormat("demo-app-worker")
                                                      .build());
        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
        worker.shutdownNow();
        log.info("Stopped");
    }

    @Override
    public void setup(InstallType type) {
        switch (type) {
            case MESH:
                log.debug("Installing mesh intents");
                worker.execute(new MeshInstaller());
                break;
            case RANDOM:
                throw new IllegalArgumentException("Not yet implemented.");
            default:
                throw new IllegalArgumentException("What is it you want exactly?");
        }
    }

    @Override
    public void tearDown() {
        worker.submit(new UnInstaller());
    }


    private class MeshInstaller implements Runnable {

        @Override
        public void run() {
            TrafficSelector selector = DefaultTrafficSelector.builder().build();
            TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

            List<Host> hosts = Lists.newArrayList(hostService.getHosts());
            while (!hosts.isEmpty()) {
                Host src = hosts.remove(0);
                for (Host dst : hosts) {
                    HostToHostIntent intent = new HostToHostIntent(appId, src.id(), dst.id(),
                                                                   selector, treatment,
                                                                   null);
                    existingIntents.add(intent);
                    intentService.submit(intent);
                }
            }
        }
    }


    private class UnInstaller implements Runnable {
        @Override
        public void run() {
            for (Intent i : existingIntents) {
                intentService.withdraw(i);
            }
        }
    }
}


