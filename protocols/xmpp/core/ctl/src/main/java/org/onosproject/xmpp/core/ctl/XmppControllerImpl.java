/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.xmpp.core.ctl;

import com.google.common.collect.Maps;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
import org.onosproject.xmpp.core.XmppController;
import org.onosproject.xmpp.core.XmppDevice;
import org.onosproject.xmpp.core.XmppDeviceId;
import org.onosproject.xmpp.core.XmppDeviceListener;
import org.onosproject.xmpp.core.XmppIqListener;
import org.onosproject.xmpp.core.XmppMessageListener;
import org.onosproject.xmpp.core.XmppPresenceListener;
import org.onosproject.xmpp.core.XmppDeviceAgent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * The main class (bundle) of XMPP protocol.
 * Responsible for:
 * 1. Initialization and starting XMPP server.
 * 2. Handling XMPP packets from clients and writing to clients.
 * 3. Configuration parameters initialization.
 * 4. Notifing listeners about XMPP events/packets.
 */
@Component(immediate = true)
@Service
public class XmppControllerImpl implements XmppController {

    private static final String APP_ID = "org.onosproject.xmpp";
    private static final String XMPP_PORT = "5269";

    private static final Logger log =
            LoggerFactory.getLogger(XmppControllerImpl.class);

    // core services declaration
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    // configuration properties definition
    @Property(name = "xmppPort", value = XMPP_PORT,
            label = "Port number used by XMPP protocol; default is 5269")
    private String xmppPort = XMPP_PORT;


    // listener declaration
    protected Set<XmppDeviceListener> xmppDeviceListeners = new CopyOnWriteArraySet<XmppDeviceListener>();

    protected Set<XmppIqListener> xmppIqListeners = new CopyOnWriteArraySet<XmppIqListener>();
    protected Set<XmppMessageListener> xmppMessageListeners = new CopyOnWriteArraySet<XmppMessageListener>();
    protected Set<XmppPresenceListener> xmppPresenceListeners = new CopyOnWriteArraySet<XmppPresenceListener>();

    protected XmppDeviceAgent agent = new DefaultXmppDeviceAgent();

    private final XmppServer xmppServer = new XmppServer();
    private DefaultXmppDeviceFactory deviceFactory = new DefaultXmppDeviceFactory();

    ConcurrentMap<XmppDeviceId, XmppDevice> connectedDevices = Maps.newConcurrentMap();
    ConcurrentMap<InetSocketAddress, XmppDeviceId> addressDeviceIdMap = Maps.newConcurrentMap();

    @Activate
    public void activate(ComponentContext context) {
        log.info("XmppControllerImpl started.");
        coreService.registerApplication(APP_ID, this::cleanup);
        cfgService.registerProperties(getClass());
        deviceFactory.init(agent);
        xmppServer.setConfiguration(context.getProperties());
        xmppServer.start(deviceFactory);
    }

    @Deactivate
    public void deactivate() {
        cleanup();
        cfgService.unregisterProperties(getClass(), false);
        log.info("Stopped");
    }

    private void cleanup() {
        xmppServer.stop();
        deviceFactory.cleanAgent();
        connectedDevices.values().forEach(XmppDevice::disconnectDevice);
        connectedDevices.clear();
    }

    @Override
    public XmppDevice getDevice(XmppDeviceId xmppDeviceId) {
        return connectedDevices.get(xmppDeviceId);
    }

    @Override
    public void addXmppDeviceListener(XmppDeviceListener deviceListener) {
        xmppDeviceListeners.add(deviceListener);
    }

    @Override
    public void removeXmppDeviceListener(XmppDeviceListener deviceListener) {
        xmppDeviceListeners.remove(deviceListener);
    }

    @Override
    public void addXmppIqListener(XmppIqListener iqListener) {
        xmppIqListeners.add(iqListener);
    }

    @Override
    public void removeXmppIqListener(XmppIqListener iqListener) {
        xmppIqListeners.remove(iqListener);
    }

    @Override
    public void addXmppMessageListener(XmppMessageListener messageListener) {
        xmppMessageListeners.add(messageListener);
    }

    @Override
    public void removeXmppMessageListener(XmppMessageListener messageListener) {
        xmppMessageListeners.remove(messageListener);
    }

    @Override
    public void addXmppPresenceListener(XmppPresenceListener presenceListener) {
        xmppPresenceListeners.add(presenceListener);
    }

    @Override
    public void removeXmppPresenceListener(XmppPresenceListener presenceListener) {
        xmppPresenceListeners.remove(presenceListener);
    }


    private class DefaultXmppDeviceAgent implements XmppDeviceAgent {

        @Override
        public boolean addConnectedDevice(XmppDeviceId deviceId, XmppDevice device) {
            if (connectedDevices.get(deviceId) != null) {
                log.warn("Trying to add Xmpp Device but found a previous " +
                        "value for XMPP deviceId: {}", deviceId);
                return false;
            } else {
                log.info("Added XMPP device: {}", deviceId);
                connectedDevices.put(deviceId, device);
                for (XmppDeviceListener listener : xmppDeviceListeners) {
                    listener.deviceConnected(deviceId);
                }
                return true;
            }
        }

        @Override
        public void removeConnectedDevice(XmppDeviceId deviceId) {
            connectedDevices.remove(deviceId);
            for (XmppDeviceListener listener : xmppDeviceListeners) {
                listener.deviceDisconnected(deviceId);
            }
        }

        @Override
        public XmppDevice getDevice(XmppDeviceId deviceId) {
            return connectedDevices.get(deviceId);
        }

        @Override
        public void processUpstreamEvent(XmppDeviceId deviceId, Packet packet) {
            if (packet instanceof IQ) {
                for (XmppIqListener iqListener : xmppIqListeners) {
                    iqListener.handleIqStanza((IQ) packet);
                }
            }
            if (packet instanceof Message) {
                for (XmppMessageListener messageListener : xmppMessageListeners) {
                    messageListener.handleMessageStanza((Message) packet);
                }
            }
            if (packet instanceof Presence) {
                for (XmppPresenceListener presenceListener : xmppPresenceListeners) {
                    presenceListener.handlePresenceStanza((Presence) packet);
                }
            }
        }

    }


}
