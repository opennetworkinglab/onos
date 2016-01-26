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
package org.onosproject.pim.impl;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages PIMInterfaces.
 *
 * TODO: Do we need to add a ServiceListener?
 */
@Component(immediate = true)
@Service
public class PIMInterfaceManager implements PIMInterfaceService {

    private final Logger log = getLogger(getClass());

    // Create ourselves a provider ID
    private static final ProviderId PID = new ProviderId("pim", "org.onosproject.pim");

    // Create a Scheduled Executor service to send PIM hellos
    private final ScheduledExecutorService helloScheduler =
            Executors.newScheduledThreadPool(1);

    // Wait for a bout 3 seconds before sending the initial hello messages.
    // TODO: make this tunnable.
    private final long initialHelloDelay = (long) 3;

    // Send PIM hello packets: 30 seconds.
    private final long pimHelloPeriod = (long) 30;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    // Store PIM Interfaces in a map key'd by ConnectPoint
    private final Map<ConnectPoint, PIMInterface> pimInterfaces = Maps.newConcurrentMap();

    @Activate
    public void activate() {
        // Query the Interface service to see if Interfaces already exist.
        log.info("Started");

        // Create PIM Interfaces for each of the existing ONOS Interfaces.
        for (Interface intf : interfaceService.getInterfaces()) {
            pimInterfaces.put(intf.connectPoint(), new PIMInterface(intf));
        }

        // Schedule the periodic hello sender.
        helloScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (PIMInterface pif : pimInterfaces.values()) {
                    pif.sendHello();
                }
            }
        }, initialHelloDelay, pimHelloPeriod, TimeUnit.SECONDS);
    }

    @Deactivate
    public void deactivate() {

        // Shutdown the periodic hello task.
        helloScheduler.shutdown();

        log.info("Stopped");
    }

    /**
     * Update the ONOS Interface with the new Interface.  If the PIMInterface does
     * not exist we'll create a new one and store it.
     *
     * @param intf ONOS Interface.
     */
    @Override
    public void updateInterface(Interface intf) {
        ConnectPoint cp = intf.connectPoint();

        log.debug("Updating Interface for " + intf.connectPoint().toString());
        pimInterfaces.compute(cp, (k, v) -> (v == null) ?
                new PIMInterface(intf) :
                v.setInterface(intf));
    }

    /**
     * Delete the PIM Interface to the corresponding ConnectPoint.
     *
     * @param cp The connect point associated with this interface we want to delete
     */
    @Override
    public void deleteInterface(ConnectPoint cp) {

        PIMInterface pi = pimInterfaces.remove(cp);
        if (pi == null) {
            log.warn("We've been asked to remove an interface we3 don't have: " + cp.toString());
            return;
        }
    }

    /**
     * Return the PIMInterface that corresponds to the given ConnectPoint.
     *
     * @param cp The ConnectPoint we want to get the PIMInterface for
     * @return The PIMInterface if it exists, NULL if it does not exist.
     */
    @Override
    public PIMInterface getPIMInterface(ConnectPoint cp) {
        PIMInterface pi = pimInterfaces.getOrDefault(cp, null);
        if (pi == null) {
            log.warn("We have been asked for an Interface we don't have: " + cp.toString());
        }
        return pi;
    }
}
