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

import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.incubator.net.config.basics.InterfaceConfig;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PIMInterfaces is a collection of all neighbors we have received
 * PIM hello messages from.  The main structure is a HashMap indexed
 * by ConnectPoint with another HashMap indexed on the PIM neighbors
 * IPAddress, it contains all PIM neighbors attached on that ConnectPoint.
 */
public final class PIMInterfaces {

    private Logger log = LoggerFactory.getLogger("PIMInterfaces");

    private static PIMInterfaces instance = null;

    // Used to listen to network configuration changes
    private NetworkConfigService configService;

    // Used to access IP Interface definitions for our segment
    private InterfaceService interfaceService;

    // Internal class used to listen for network configuration changes
    private InternalConfigListener configListener = new InternalConfigListener();

    // This is the global container for all PIM Interfaces indexed by ConnectPoints.
    private Map<ConnectPoint, PIMInterface> interfaces = new HashMap<>();

    // Default hello message interval
    private int helloMessageInterval = 60;

    // Timer used to send hello messages on this interface
    private Timeout helloTimer;

    // Required by a utility class
    private PIMInterfaces() {}

    /**
     * Get the instance of PIMInterfaces.  Create the instance if needed.
     *
     * @return PIMInterface instance
     */
    public static PIMInterfaces getInstance() {
        if (null == instance) {
            instance = new PIMInterfaces();
        }
        return instance;
    }

    // Initialize the services
    public void initialize(NetworkConfigService cs, InterfaceService is) {
        configService = cs;
        interfaceService = is;

        // Initialize interfaces if they already exist
        initInterfaces();

        // Listen for network config changes
        configService.addListener(configListener);
    }

    /**
     * Listener for network config events.
     */
    private class InternalConfigListener implements NetworkConfigListener {

        private void updateInterfaces(InterfaceConfig config) {
            Set<Interface> intfs;
            try {
                intfs = config.getInterfaces();
            } catch (ConfigException e) {
                log.error(e.toString());
                return;
            }
            for (Interface intf : intfs) {
                addInterface(intf);
            }
        }

        /**
         * Remove the PIMInterface represented by the ConnectPoint. If the
         * PIMInterface does not exist this function is a no-op.
         *
         * @param cp The connectPoint representing the PIMInterface to be removed.
         */
        private void removeInterface(ConnectPoint cp) {
            PIMInterfaces.this.removeInterface(cp);
        }

        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    log.debug("Config updated: " + event.toString() + "\n");
                    if (event.configClass() == InterfaceConfig.class) {
                        InterfaceConfig config =
                                configService.getConfig((ConnectPoint) event.subject(), InterfaceConfig.class);
                        updateInterfaces(config);
                    }
                    break;
                case CONFIG_REMOVED:
                    if (event.configClass() == InterfaceConfig.class) {
                        removeInterface((ConnectPoint) event.subject());
                    }
                    break;
                case CONFIG_REGISTERED:
                case CONFIG_UNREGISTERED:
                default:
                    break;
            }
        }
    }

    // Configure interfaces if they already exist.
    private void initInterfaces() {
        Set<Interface> intfs = interfaceService.getInterfaces();
        for (Interface intf : intfs) {
            log.debug("Adding interface: " + intf.toString() + "\n");
            addInterface(intf);
        }
    }

    /**
     * Create a PIM Interface and add to our interfaces list.
     *
     * @param intf the interface to add
     * @return the PIMInterface
     */
    public PIMInterface addInterface(Interface intf) {
        PIMInterface pif = new PIMInterface(intf);
        interfaces.put(intf.connectPoint(), pif);

        // If we have added our first interface start the hello timer.
        if (interfaces.size() == 1) {
            startHelloTimer();
        }

        // Return this interface
        return pif;
    }

    /**
     * Remove the PIMInterface from the given ConnectPoint.
     *
     * @param cp the ConnectPoint indexing the PIMInterface to be removed.
     */
    public void removeInterface(ConnectPoint cp) {
        if (interfaces.containsKey(cp)) {
            interfaces.remove(cp);
        }

        if (interfaces.size() == 0) {
            PIMTimer.stop();
        }
    }

    /**
     * Return a collection of PIMInterfaces for use by the PIM Interface codec.
     *
     * @return the collection of PIMInterfaces
     */
    public Collection<PIMInterface> getInterfaces() {
        return interfaces.values();
    }

    /**
     * Get the PIM Interface indexed by the given ConnectPoint.
     *
     * @param cp the connect point
     * @return the PIMInterface if it exists, NULL if not
     */
    public PIMInterface getInterface(ConnectPoint cp) {
        return interfaces.get(cp);
    }

    /**
     * Return a string of PIMInterfaces for the cli command.
     *
     * @return a string representing PIM interfaces
     */
    public String printInterfaces() {
        String str = "";
        for (PIMInterface pi : interfaces.values()) {
            str += pi.toString();
        }
        return str;
    }

    /* ---------------------------------- PIM Hello Timer ----------------------------------- */

    /**
     * Start a new hello timer for this interface.
     */
    private void startHelloTimer() {
        helloTimer = PIMTimer.getTimer().newTimeout(
                new HelloTimer(),
                helloMessageInterval,
                TimeUnit.SECONDS);

        log.debug("Started Hello Timer");
    }

    /**
     * This inner class handles transmitting a PIM hello message on this ConnectPoint.
     */
    private final class HelloTimer implements TimerTask {

        HelloTimer() {
        }

        @Override
        public void run(Timeout timeout) throws Exception {

            log.debug("Running Hello Timer\n");
            // Technically we should not send all hello's in synch..
            for (PIMInterface pi : interfaces.values()) {
                pi.sendHello();
            }

            // restart the hello timer
            if (interfaces.size() > 0) {
                startHelloTimer();
            }
        }
    }
}