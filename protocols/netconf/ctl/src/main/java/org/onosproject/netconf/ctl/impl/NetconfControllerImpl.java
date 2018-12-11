/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.netconf.ctl.impl;

import org.apache.commons.lang3.tuple.Triple;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.key.DeviceKeyService;
import org.onosproject.net.key.UsernamePassword;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceFactory;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceListener;
import org.onosproject.netconf.NetconfDeviceOutputEvent;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.config.NetconfDeviceConfig;
import org.onosproject.netconf.config.NetconfSshClientLib;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;
import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.getIntegerProperty;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.netconf.NetconfDeviceInfo.extractIpPortPath;

/**
 * The implementation of NetconfController.
 */
@Component(immediate = true)
@Service
public class NetconfControllerImpl implements NetconfController {

    protected static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
    private static final String PROP_NETCONF_CONNECT_TIMEOUT = "netconfConnectTimeout";
    // FIXME @Property should not be static
    @Property(name = PROP_NETCONF_CONNECT_TIMEOUT, intValue = DEFAULT_CONNECT_TIMEOUT_SECONDS,
            label = "Time (in seconds) to wait for a NETCONF connect.")
    protected static int netconfConnectTimeout = DEFAULT_CONNECT_TIMEOUT_SECONDS;

    private static final String PROP_NETCONF_REPLY_TIMEOUT = "netconfReplyTimeout";
    protected static final int DEFAULT_REPLY_TIMEOUT_SECONDS = 5;
    // FIXME @Property should not be static
    @Property(name = PROP_NETCONF_REPLY_TIMEOUT, intValue = DEFAULT_REPLY_TIMEOUT_SECONDS,
            label = "Time (in seconds) waiting for a NetConf reply")
    protected static int netconfReplyTimeout = DEFAULT_REPLY_TIMEOUT_SECONDS;

    private static final String PROP_NETCONF_IDLE_TIMEOUT = "netconfIdleTimeout";
    protected static final int DEFAULT_IDLE_TIMEOUT_SECONDS = 300;
    // FIXME @Property should not be static
    @Property(name = PROP_NETCONF_IDLE_TIMEOUT, intValue = DEFAULT_IDLE_TIMEOUT_SECONDS,
            label = "Time (in seconds) SSH session will close if no traffic seen")
    protected static int netconfIdleTimeout = DEFAULT_IDLE_TIMEOUT_SECONDS;

    private static final String SSH_LIBRARY = "sshLibrary";
    private static final String APACHE_MINA_STR = "apache-mina";
    @Property(name = SSH_LIBRARY, value = APACHE_MINA_STR,
            label = "Ssh client library to use")
    protected NetconfSshClientLib sshLibrary = NetconfSshClientLib.APACHE_MINA;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceKeyService deviceKeyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    public static final Logger log = LoggerFactory
            .getLogger(NetconfControllerImpl.class);

    private Map<DeviceId, NetconfDevice> netconfDeviceMap = new ConcurrentHashMap<>();

    private Map<DeviceId, Lock> netconfCreateMutex = new ConcurrentHashMap<>();

    private final NetconfDeviceOutputEventListener downListener = new DeviceDownEventListener();

    protected Set<NetconfDeviceListener> netconfDeviceListeners = new CopyOnWriteArraySet<>();
    protected NetconfDeviceFactory deviceFactory = (deviceInfo) -> new DefaultNetconfDevice(deviceInfo);

    protected final ExecutorService executor =
            Executors.newCachedThreadPool(groupedThreads("onos/netconfdevicecontroller",
                                                         "connection-reopen-%d", log));

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        modified(context);
        Security.addProvider(new BouncyCastleProvider());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        netconfDeviceMap.values().forEach(device -> {
            device.getSession().removeDeviceOutputListener(downListener);
            device.disconnect();
        });
        cfgService.unregisterProperties(getClass(), false);
        netconfDeviceListeners.clear();
        netconfDeviceMap.clear();
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            netconfReplyTimeout = DEFAULT_REPLY_TIMEOUT_SECONDS;
            netconfConnectTimeout = DEFAULT_CONNECT_TIMEOUT_SECONDS;
            netconfIdleTimeout = DEFAULT_IDLE_TIMEOUT_SECONDS;
            sshLibrary = NetconfSshClientLib.APACHE_MINA;
            log.info("No component configuration");
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();

        String newSshLibrary;

        int newNetconfReplyTimeout = getIntegerProperty(
                properties, PROP_NETCONF_REPLY_TIMEOUT, netconfReplyTimeout);
        int newNetconfConnectTimeout = getIntegerProperty(
                properties, PROP_NETCONF_CONNECT_TIMEOUT, netconfConnectTimeout);
        int newNetconfIdleTimeout = getIntegerProperty(
                properties, PROP_NETCONF_IDLE_TIMEOUT, netconfIdleTimeout);

        newSshLibrary = get(properties, SSH_LIBRARY);

        if (newNetconfConnectTimeout < 0) {
            log.warn("netconfConnectTimeout is invalid - less than 0");
            return;
        } else if (newNetconfReplyTimeout <= 0) {
            log.warn("netconfReplyTimeout is invalid - 0 or less.");
            return;
        } else if (newNetconfIdleTimeout <= 0) {
            log.warn("netconfIdleTimeout is invalid - 0 or less.");
            return;
        }

        netconfReplyTimeout = newNetconfReplyTimeout;
        netconfConnectTimeout = newNetconfConnectTimeout;
        netconfIdleTimeout = newNetconfIdleTimeout;
        if (newSshLibrary != null) {
            sshLibrary = NetconfSshClientLib.getEnum(newSshLibrary);
        }
        log.info("Settings: {} = {}, {} = {}, {} = {}, {} = {}",
                 PROP_NETCONF_REPLY_TIMEOUT, netconfReplyTimeout,
                 PROP_NETCONF_CONNECT_TIMEOUT, netconfConnectTimeout,
                 PROP_NETCONF_IDLE_TIMEOUT, netconfIdleTimeout,
                 SSH_LIBRARY, sshLibrary);
    }

    @Override
    public void addDeviceListener(NetconfDeviceListener listener) {
        if (!netconfDeviceListeners.contains(listener)) {
            netconfDeviceListeners.add(listener);
        }
    }

    @Override
    public void removeDeviceListener(NetconfDeviceListener listener) {
        netconfDeviceListeners.remove(listener);
    }

    @Override
    public NetconfDevice getNetconfDevice(DeviceId deviceInfo) {
        return netconfDeviceMap.get(deviceInfo);
    }

    @Override
    public NetconfDevice getNetconfDevice(IpAddress ip, int port, String path) {
        return getNetconfDevice(DeviceId.deviceId(
                    String.format("netconf:%s:%d%s",
                        ip.toString(), port, (path != null && !path.isEmpty() ? "/" + path : ""))));
    }

    @Override
    public NetconfDevice getNetconfDevice(IpAddress ip, int port) {
        return getNetconfDevice(ip, port, null);
    }

    @Override
    public NetconfDevice connectDevice(DeviceId deviceId) throws NetconfException {
        NetconfDeviceConfig netCfg  = netCfgService.getConfig(
                deviceId, NetconfDeviceConfig.class);
        NetconfDeviceInfo deviceInfo = null;

        /*
         * A bit of an ugly race condition can be found here. It is possible
         * that this method is called to create a connection to device A and
         * while that device is in the process of being created another call
         * to this method for A will be invoked. Since the first call to
         * create A has not been completed device A is not in the the
         * netconfDeviceMap yet.
         *
         * To prevent this situation a mutex is introduced so that the first
         * call will be allowed to complete before the second is processed.
         * The mutex is based on the device ID, so that it should be still
         * possible to connect to different devices concurrently.
         */
        Lock mutex;
        synchronized (netconfCreateMutex) {
            mutex = netconfCreateMutex.get(deviceId);
            if (mutex == null) {
                mutex = new ReentrantLock();
                netconfCreateMutex.put(deviceId, mutex);
            }
        }
        mutex.lock();
        try {
            if (netconfDeviceMap.containsKey(deviceId)) {
                log.debug("Device {} is already present", deviceId);
                return netconfDeviceMap.get(deviceId);
            } else if (netCfg != null) {
                log.debug("Device {} is present in NetworkConfig", deviceId);
                deviceInfo = new NetconfDeviceInfo(netCfg);
            } else {
                log.debug("Creating NETCONF device {}", deviceId);
                Device device = deviceService.getDevice(deviceId);
                String ip, path = null;
                int port;
                if (device != null) {
                    ip = device.annotations().value("ipaddress");
                    port = Integer.parseInt(device.annotations().value("port"));
                    path = device.annotations().value("path");
                } else {
                    Triple<String, Integer, Optional<String>> info = extractIpPortPath(deviceId);
                    ip = info.getLeft();
                    port = info.getMiddle();
                    path = (info.getRight().isPresent() ? info.getRight().get() : null);
                }
                try {
                    DeviceKey deviceKey = deviceKeyService.getDeviceKey(
                            DeviceKeyId.deviceKeyId(deviceId.toString()));
                    if (deviceKey.type() == DeviceKey.Type.USERNAME_PASSWORD) {
                        UsernamePassword usernamepasswd = deviceKey.asUsernamePassword();

                        deviceInfo = new NetconfDeviceInfo(usernamepasswd.username(),
                                                           usernamepasswd.password(),
                                                           IpAddress.valueOf(ip),
                                                           port,
                                                           path);

                    } else if (deviceKey.type() == DeviceKey.Type.SSL_KEY) {
                        String username = deviceKey.annotations().value(AnnotationKeys.USERNAME);
                        String password = deviceKey.annotations().value(AnnotationKeys.PASSWORD);
                        String sshkey = deviceKey.annotations().value(AnnotationKeys.SSHKEY);

                        deviceInfo = new NetconfDeviceInfo(username,
                                                           password,
                                                           IpAddress.valueOf(ip),
                                                           port,
                                                           path,
                                                           sshkey);
                    } else {
                        log.error("Unknown device key for device {}", deviceId);
                    }
                } catch (NullPointerException e) {
                    throw new NetconfException("No Device Key for device " + deviceId, e);
                }
            }
            NetconfDevice netconfDevicedevice = createDevice(deviceInfo);
            netconfDevicedevice.getSession().addDeviceOutputListener(downListener);
            return netconfDevicedevice;
        } finally {
            mutex.unlock();
        }
    }

    @Override
    public void disconnectDevice(DeviceId deviceId, boolean remove) {
        if (!netconfDeviceMap.containsKey(deviceId)) {
            log.debug("Device {} is not present", deviceId);
        } else {
            stopDevice(deviceId, remove);
        }
    }

    private void stopDevice(DeviceId deviceId, boolean remove) {
        Lock mutex;
        synchronized (netconfCreateMutex) {
            mutex = netconfCreateMutex.remove(deviceId);
        }
        NetconfDevice nc;
        if (mutex == null) {
            log.warn("Unexpected stoping a device that has no lock");
            nc = netconfDeviceMap.remove(deviceId);
        } else {
            mutex.lock();
            try {
                nc = netconfDeviceMap.remove(deviceId);
            } finally {
                mutex.unlock();
            }
        }
        if (nc != null) {
            nc.disconnect();
        }
        if (remove) {
            for (NetconfDeviceListener l : netconfDeviceListeners) {
                l.deviceRemoved(deviceId);
            }
        }
    }

    @Override
    public void removeDevice(DeviceId deviceId) {
        if (!netconfDeviceMap.containsKey(deviceId)) {
            log.warn("Device {} is not present", deviceId);
            for (NetconfDeviceListener l : netconfDeviceListeners) {
                l.deviceRemoved(deviceId);
            }
        } else {
            stopDevice(deviceId, true);
        }
    }

    private NetconfDevice createDevice(NetconfDeviceInfo deviceInfo) throws NetconfException {
        NetconfDevice netconfDevice = deviceFactory.createNetconfDevice(deviceInfo);
        netconfDeviceMap.put(deviceInfo.getDeviceId(), netconfDevice);
        for (NetconfDeviceListener l : netconfDeviceListeners) {
            l.deviceAdded(deviceInfo.getDeviceId());
        }
        return netconfDevice;
    }


    @Override
    public Map<DeviceId, NetconfDevice> getDevicesMap() {
        return netconfDeviceMap;
    }

    @Override
    public Set<DeviceId> getNetconfDevices() {
        return netconfDeviceMap.keySet();
    }


    /**
     * Device factory for the specific NetconfDeviceImpl.
     *
     * @deprecated in 1.14.0
     */
    @Deprecated
    private class DefaultNetconfDeviceFactory implements NetconfDeviceFactory {

        @Override
        public NetconfDevice createNetconfDevice(NetconfDeviceInfo netconfDeviceInfo)
                throws NetconfException {
            return new DefaultNetconfDevice(netconfDeviceInfo);
        }
    }

    //Listener for closed session with devices, gets triggered when devices goes down
    // or sends the end pattern ]]>]]>
    private class DeviceDownEventListener implements NetconfDeviceOutputEventListener {

        @Override
        public void event(NetconfDeviceOutputEvent event) {
            DeviceId did = event.getDeviceInfo().getDeviceId();
            if (event.type().equals(NetconfDeviceOutputEvent.Type.DEVICE_UNREGISTERED) ||
                    !mastershipService.isLocalMaster(did)) {
                removeDevice(did);
            } else if (event.type().equals(NetconfDeviceOutputEvent.Type.SESSION_CLOSED)) {
                log.info("Trying to reestablish connection with device {}", did);
                executor.execute(() -> {
                    try {
                        NetconfDevice device = netconfDeviceMap.get(did);
                        if (device != null) {
                            device.getSession().checkAndReestablish();
                            log.info("Connection with device {} was reestablished", did);
                        } else {
                            log.warn("The device {} is not in the system", did);
                        }

                    } catch (NetconfException e) {
                        log.error("The SSH connection with device {} couldn't be " +
                                "reestablished due to {}. " +
                                "Marking the device as unreachable", e.getMessage());
                        log.debug("Complete exception: ", e);
                        removeDevice(did);
                    }
                });
            }
        }

        @Override
        public boolean isRelevant(NetconfDeviceOutputEvent event) {
            return getDevicesMap().containsKey(event.getDeviceInfo().getDeviceId());
        }
    }
}
