/*
 * Copyright 2019-present Open Networking Foundation
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

import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;

import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
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
import org.onosproject.netconf.NetconfProxyMessage;
import org.onosproject.netconf.NetconfProxyMessageHandler;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.config.NetconfDeviceConfig;
import org.onosproject.netconf.config.NetconfSshClientLib;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.osgi.service.component.ComponentContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.getIntegerProperty;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.netconf.ctl.impl.OsgiPropertyConstants.*;
import static org.onosproject.netconf.NetconfDeviceInfo.extractIpPortPath;

/**
 * The implementation of NetconfController.
 */
@Component(immediate = true, service = NetconfController.class,
        property = {
                NETCONF_CONNECT_TIMEOUT + ":Integer=" + NETCONF_CONNECT_TIMEOUT_DEFAULT,
                NETCONF_REPLY_TIMEOUT + ":Integer=" + NETCONF_REPLY_TIMEOUT_DEFAULT,
                NETCONF_IDLE_TIMEOUT + ":Integer=" + NETCONF_IDLE_TIMEOUT_DEFAULT,
                SSH_LIBRARY + "=" + SSH_LIBRARY_DEFAULT,
                SSH_KEY_PATH + "=" + SSH_KEY_PATH_DEFAULT,
        })
public class NetconfControllerImpl implements NetconfController {

    /** Time (in seconds) to wait for a NETCONF connect. */
    protected static int netconfConnectTimeout = NETCONF_CONNECT_TIMEOUT_DEFAULT;

    /** Time (in seconds) waiting for a NetConf reply. */
    protected static int netconfReplyTimeout = NETCONF_REPLY_TIMEOUT_DEFAULT;

    /** Time (in seconds) SSH session will close if no traffic seen. */
    protected static int netconfIdleTimeout = NETCONF_IDLE_TIMEOUT_DEFAULT;

    /** SSH client library to use. */
    protected static String sshLibrary = SSH_LIBRARY_DEFAULT;

    /** Private SSH Key File Path to use. */
    protected static String sshKeyPath = SSH_KEY_PATH_DEFAULT;

    protected NetconfSshClientLib sshClientLib = NetconfSshClientLib.APACHE_MINA;

    private static final MessageSubject SEND_REQUEST_SUBJECT_STRING =
            new MessageSubject("netconf-session-master-send-message-string");

    private static final MessageSubject SEND_REPLY_SUBJECT_STRING =
            new MessageSubject("netconf-session-master-send-reply-message-string");

    private static final MessageSubject SEND_REQUEST_SUBJECT_SET_STRING =
            new MessageSubject("netconf-session-master-send-message-set-string");

    private static final MessageSubject SEND_REPLY_SUBJECT_SET_STRING =
            new MessageSubject("netconf-session-master-send-reply-message-set-string");


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceKeyService deviceKeyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry netCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    public static final Logger log = LoggerFactory
            .getLogger(NetconfControllerImpl.class);

    private static final int CORE_POOL_SIZE = 32;

    private Map<DeviceId, NetconfDevice> netconfDeviceMap = new ConcurrentHashMap<>();

    private Map<DeviceId, Lock> netconfCreateMutex = new ConcurrentHashMap<>();

    private final NetconfDeviceOutputEventListener downListener = new DeviceDownEventListener();

    protected Set<NetconfDeviceListener> netconfDeviceListeners = new CopyOnWriteArraySet<>();
    protected NetconfDeviceFactory deviceFactory = new DefaultNetconfDeviceFactory();

    protected NetconfProxyMessageHandler netconfProxyMessageHandler = new NetconfProxyMessageHandlerImpl();


    protected final ExecutorService executor =
            Executors.newFixedThreadPool(CORE_POOL_SIZE, groupedThreads("onos/netconfdevicecontroller",
                                                         "connection-reopen-%d", log));

    private final ExecutorService remoteRequestExecutor = Executors.newFixedThreadPool(CORE_POOL_SIZE);

    protected NodeId localNodeId;

    private CountDownLatch countDownLatch;

    private ArrayList<String> replyArguments = new ArrayList<>();

    public static final Serializer SERIALIZER = Serializer.using(
            KryoNamespace.newBuilder()
                    .register(KryoNamespaces.API)
                    .register(NetconfProxyMessage.class)
                    .register(NetconfProxyMessage.SubjectType.class)
                    .register(DefaultNetconfProxyMessage.class)
                    .register(String.class)
                    .build("NetconfProxySession"));

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        modified(context);
        Security.addProvider(new BouncyCastleProvider());
        clusterCommunicator.<NetconfProxyMessage>addSubscriber(
                SEND_REQUEST_SUBJECT_STRING,
                SERIALIZER::decode,
                this::handleProxyMessage,
                remoteRequestExecutor);
        clusterCommunicator.<NetconfProxyMessage>addSubscriber(
                SEND_REQUEST_SUBJECT_SET_STRING,
                SERIALIZER::decode,
                this::handleProxyMessage,
                remoteRequestExecutor);
        clusterCommunicator.<NetconfProxyMessage>addSubscriber(
                SEND_REPLY_SUBJECT_STRING,
                SERIALIZER::decode,
                this::handleProxyReplyMessage,
                remoteRequestExecutor);
        clusterCommunicator.<NetconfProxyMessage>addSubscriber(
                SEND_REPLY_SUBJECT_SET_STRING,
                SERIALIZER::decode,
                this::handleProxyReplyMessage,
                remoteRequestExecutor);

        localNodeId = Optional.ofNullable(clusterService.getLocalNode())
                .map(ControllerNode::id)
                .orElseGet(() -> new NodeId("nullNodeId"));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        netconfDeviceMap.values().forEach(device -> {
            if (device.isMasterSession()) {
                try {
                    device.getSession().removeDeviceOutputListener(downListener);
                } catch (NetconfException e) {
                    log.error("removeDeviceOutputListener Failed {}", e.getMessage());
                }
            }
            device.disconnect();
        });
        clusterCommunicator.removeSubscriber(SEND_REQUEST_SUBJECT_STRING);
        clusterCommunicator.removeSubscriber(SEND_REQUEST_SUBJECT_SET_STRING);
        clusterCommunicator.removeSubscriber(SEND_REPLY_SUBJECT_STRING);
        clusterCommunicator.removeSubscriber(SEND_REPLY_SUBJECT_SET_STRING);
        cfgService.unregisterProperties(getClass(), false);
        netconfDeviceListeners.clear();
        netconfDeviceMap.clear();
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            netconfReplyTimeout = NETCONF_REPLY_TIMEOUT_DEFAULT;
            netconfConnectTimeout = NETCONF_CONNECT_TIMEOUT_DEFAULT;
            netconfIdleTimeout = NETCONF_IDLE_TIMEOUT_DEFAULT;
            sshLibrary = SSH_LIBRARY_DEFAULT;
            sshKeyPath = SSH_KEY_PATH_DEFAULT;
            sshClientLib = NetconfSshClientLib.APACHE_MINA;
            log.info("No component configuration");
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();

        String newSshLibrary;
        String newSshKeyPath;

        int newNetconfReplyTimeout = getIntegerProperty(
                properties, NETCONF_REPLY_TIMEOUT, netconfReplyTimeout);
        int newNetconfConnectTimeout = getIntegerProperty(
                properties, NETCONF_CONNECT_TIMEOUT, netconfConnectTimeout);
        int newNetconfIdleTimeout = getIntegerProperty(
                properties, NETCONF_IDLE_TIMEOUT, netconfIdleTimeout);

        newSshLibrary = get(properties, SSH_LIBRARY);
        newSshKeyPath = get(properties, SSH_KEY_PATH);

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
            sshLibrary = newSshLibrary;
            sshClientLib = NetconfSshClientLib.getEnum(newSshLibrary);
        }
        if (newSshKeyPath != null) {
            sshKeyPath = newSshKeyPath;
        }
        log.info("Settings: {} = {}, {} = {}, {} = {}, {} = {}, {} = {}",
                 NETCONF_REPLY_TIMEOUT, netconfReplyTimeout,
                 NETCONF_CONNECT_TIMEOUT, netconfConnectTimeout,
                 NETCONF_IDLE_TIMEOUT, netconfIdleTimeout,
                 SSH_LIBRARY, sshLibrary,
                 SSH_KEY_PATH, sshKeyPath);
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
        return connectDevice(deviceId, true);
    }

    @Override
    public NetconfDevice connectDevice(DeviceId deviceId, boolean isMaster) throws NetconfException {
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
                //If not master or already has session: return, otherwise create device again.
                if (!isMaster || netconfDeviceMap.get(deviceId).isMasterSession()) {
                    log.debug("Device {} is already present", deviceId);
                    return netconfDeviceMap.get(deviceId);
                }
            }

            if (netCfg != null) {
                log.debug("Device {} is present in NetworkConfig", deviceId);
                deviceInfo = new NetconfDeviceInfo(netCfg, deviceId);
            } else {
                log.debug("Creating NETCONF device {}", deviceId);
                deviceInfo = createDeviceInfo(deviceId);
            }
            NetconfDevice netconfDevice = createDevice(deviceInfo, isMaster);
            if (isMaster) {
                netconfDevice.getSession().addDeviceOutputListener(downListener);
            }
            return netconfDevice;
        } finally {

            mutex.unlock();
        }
    }

    @Override
    public NodeId getLocalNodeId() {
        return localNodeId;
    }

    private NetconfDeviceInfo createDeviceInfo(DeviceId deviceId) throws NetconfException {
            Device device = deviceService.getDevice(deviceId);
            String ip, path = null;
            int port;
            if (device != null) {
                ip = device.annotations().value("ipaddress");
                port = Integer.parseInt(device.annotations().value("port"));
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

                    return new NetconfDeviceInfo(usernamepasswd.username(),
                                                       usernamepasswd.password(),
                                                       IpAddress.valueOf(ip),
                                                       port,
                                                       path);

                } else if (deviceKey.type() == DeviceKey.Type.SSL_KEY) {
                    String username = deviceKey.annotations().value(AnnotationKeys.USERNAME);
                    String password = deviceKey.annotations().value(AnnotationKeys.PASSWORD);
                    String sshkey = deviceKey.annotations().value(AnnotationKeys.SSHKEY);

                    return new NetconfDeviceInfo(username,
                                                       password,
                                                       IpAddress.valueOf(ip),
                                                       port,
                                                       path,
                                                       sshkey);
                } else {
                    log.error("Unknown device key for device {}", deviceId);
                    throw new NetconfException("Unknown device key for device " + deviceId);
                }
            } catch (NullPointerException e) {
                log.error("No Device Key for device {}, {}", deviceId, e);
                throw new NetconfException("No Device Key for device " + deviceId, e);
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
        return createDevice(deviceInfo, true);
    }

    private NetconfDevice createDevice(NetconfDeviceInfo deviceInfo,
                                       boolean isMaster) throws NetconfException {
        NetconfDevice netconfDevice = deviceFactory.createNetconfDevice(deviceInfo, isMaster);
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

    private void unicastRpcToMaster(NetconfProxyMessage proxyMessage, NodeId receiverId) {
        MessageSubject messageSubject;

        switch (proxyMessage.subjectType()) {
            case GET_DEVICE_CAPABILITIES_SET:
                messageSubject = SEND_REQUEST_SUBJECT_SET_STRING;
                break;
            default:
                messageSubject = SEND_REQUEST_SUBJECT_STRING;
                break;
        }

        clusterCommunicator
                .unicast(proxyMessage,
                         messageSubject,
                         SERIALIZER::encode,
                         receiverId);
    }

    private void unicastReplyToSender(NetconfProxyMessage proxyMessage, NodeId receiverId) {
        MessageSubject messageSubject;

        switch (proxyMessage.subjectType()) {
            case GET_DEVICE_CAPABILITIES_SET:
                messageSubject = SEND_REPLY_SUBJECT_SET_STRING;
                break;
            default:
                messageSubject = SEND_REPLY_SUBJECT_STRING;
                break;
        }

        clusterCommunicator
                .unicast(proxyMessage,
                         messageSubject,
                         SERIALIZER::encode,
                         receiverId);
    }

    @Override
    public <T> CompletableFuture<T> executeAtMaster(NetconfProxyMessage proxyMessage) throws NetconfException {
        DeviceId deviceId = proxyMessage.deviceId();
        if (deviceService.getRole(deviceId).equals(MastershipRole.MASTER)) {
            return handleProxyMessage(proxyMessage);
        } else {
            return relayMessageToMaster(proxyMessage);
        }
    }

    @Override
    public boolean pingDevice(DeviceId deviceId) {
        try {
            CompletableFuture<CharSequence> future = getNetconfDevice(deviceId).getSession().asyncGet();
            CharSequence reply = Tools.futureGetOrElse(future, NETCONF_REPLY_TIMEOUT_DEFAULT, TimeUnit.SECONDS,
                                  "Unable to read netconf data tree from device");
            if (reply.equals("Unable to read netconf data tree from device")) {
                log.error("Failed to get netconf data tree for device : {}", deviceId);
                return false;
            }
            log.debug("Netconf data tree for device : {} -> {}", deviceId, reply);
        } catch (NetconfException e) {
            log.error("Error while getting netconf data tree for device : {}", deviceId);
            log.error("Error details : ", e);
            return false;
        }
        return true;
    }

    public <T> CompletableFuture<T> relayMessageToMaster(NetconfProxyMessage proxyMessage) {
        DeviceId deviceId = proxyMessage.deviceId();

        countDownLatch = new CountDownLatch(1);
        unicastRpcToMaster(proxyMessage, mastershipService.getMasterFor(deviceId));

        try {
            countDownLatch.await(netconfReplyTimeout, TimeUnit.SECONDS);

            switch (proxyMessage.subjectType()) {
                case GET_DEVICE_CAPABILITIES_SET:
                    Set<String> forReturnValue = new LinkedHashSet<>(replyArguments);
                    return CompletableFuture.completedFuture((T) forReturnValue);
                default:
                    String returnValue = null;
                    if (!replyArguments.isEmpty()) {
                        returnValue = Optional.ofNullable(replyArguments.get(0)).orElse(null);
                    }
                    return CompletableFuture.completedFuture((T) returnValue);
            }
        } catch (InterruptedException e) {
            log.error("InterruptedOccured while awaiting because of {}", e);
            CompletableFuture<T> errorFuture = new CompletableFuture<>();
            errorFuture.completeExceptionally(e);
            return errorFuture;
        } catch (Exception e) {
            log.error("Exception occured because of {}", e);
            CompletableFuture<T> errorFuture = new CompletableFuture<>();
            errorFuture.completeExceptionally(e);
            return errorFuture;
        }
    }

    private <T> CompletableFuture<T> handleProxyMessage(NetconfProxyMessage proxyMessage) {
        countDownLatch = new CountDownLatch(1);
        try {
            switch (proxyMessage.subjectType()) {
                case GET_DEVICE_CAPABILITIES_SET:
                    return CompletableFuture.completedFuture(
                            (T) netconfProxyMessageHandler.handleIncomingSetMessage(proxyMessage));
                default:
                    return CompletableFuture.completedFuture(
                            netconfProxyMessageHandler.handleIncomingMessage(proxyMessage));
            }
        } catch (NetconfException e) {
            CompletableFuture<T> errorFuture = new CompletableFuture<>();
            errorFuture.completeExceptionally(e);
            return errorFuture;
        }
    }

    private <T> CompletableFuture<T> handleProxyReplyMessage(NetconfProxyMessage replyMessage) {
        try {
            switch (replyMessage.subjectType()) {
                case GET_DEVICE_CAPABILITIES_SET:
                    return CompletableFuture.completedFuture(
                            (T) netconfProxyMessageHandler.handleReplySetMessage(replyMessage));
                default:
                    return CompletableFuture.completedFuture(
                            netconfProxyMessageHandler.handleReplyMessage(replyMessage));

            }
        } catch (NetconfException e) {
            CompletableFuture<T> errorFuture = new CompletableFuture<>();
            errorFuture.completeExceptionally(e);
            return errorFuture;
        }
    }


    /**
     * Netconf Proxy Message Handler Implementation class.
     */
    private class NetconfProxyMessageHandlerImpl implements NetconfProxyMessageHandler {

        @Override
        public <T> T handleIncomingMessage(NetconfProxyMessage proxyMessage) throws NetconfException {
            //TODO: Should throw Netconf Exception in error cases?
            DeviceId deviceId = proxyMessage.deviceId();
            NetconfProxyMessage.SubjectType subjectType = proxyMessage.subjectType();
            NetconfSession secureTransportSession;

            if (netconfDeviceMap.get(deviceId) != null && netconfDeviceMap.get(deviceId).isMasterSession()) {
                secureTransportSession = netconfDeviceMap.get(deviceId).getSession();
            } else {
                throw new NetconfException("Ssh session not present");
            }
            T reply = null;
            ArrayList<String> arguments = Lists.newArrayList(proxyMessage.arguments());
            try {
                switch (subjectType) {
                    case RPC:
                        reply = (T) secureTransportSession.rpc(arguments.get(0))
                                .get(netconfReplyTimeout, TimeUnit.SECONDS);
                        break;
                    case REQUEST_SYNC:
                        reply = (T) secureTransportSession.requestSync(arguments.get(0));
                        break;
                    case START_SUBSCRIPTION:
                        secureTransportSession.startSubscription(arguments.get(0));
                        break;
                    case END_SUBSCRIPTION:
                        secureTransportSession.endSubscription();
                        break;
                    case REQUEST:
                        reply = (T) secureTransportSession.request(arguments.get(0))
                                .get(netconfReplyTimeout, TimeUnit.SECONDS);
                        break;
                    case GET_SESSION_ID:
                        reply = (T) secureTransportSession.getSessionId();
                        break;
                    case GET_DEVICE_CAPABILITIES_SET:
                        reply = (T) secureTransportSession.getDeviceCapabilitiesSet();
                        break;
                    default:
                        log.error("Not yet supported for session method {}", subjectType);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NetconfException(e.getMessage(), e.getCause());
            } catch (ExecutionException | TimeoutException e) {
                throw new NetconfException(e.getMessage(), e.getCause());
            }

            ArrayList<String> returnArgument = new ArrayList<String>();
            Optional.ofNullable(reply).ifPresent(r -> returnArgument.add((String) r));

            DefaultNetconfProxyMessage replyMessage = new DefaultNetconfProxyMessage(
                    subjectType,
                    deviceId,
                    returnArgument,
                    localNodeId);

            unicastReplyToSender(replyMessage, proxyMessage.senderId());


            return reply;
        }

        @Override
        public <T> T handleReplyMessage(NetconfProxyMessage replyMessage) {
            replyArguments = new ArrayList<>(replyMessage.arguments());
            countDownLatch.countDown();
            return (T) Optional.ofNullable(replyArguments.get(0)).orElse(null);
        }

        @Override
        public Set<String> handleIncomingSetMessage(NetconfProxyMessage proxyMessage) throws NetconfException {
            DeviceId deviceId = proxyMessage.deviceId();
            NetconfProxyMessage.SubjectType subjectType = proxyMessage.subjectType();
            NetconfSession secureTransportSession;

            if (netconfDeviceMap.get(deviceId) != null && netconfDeviceMap.get(deviceId).isMasterSession()) {
                secureTransportSession = netconfDeviceMap.get(deviceId).getSession();
            } else {
                throw new NetconfException("SSH session not present");
            }

            Set<String> reply = secureTransportSession.getDeviceCapabilitiesSet();
            ArrayList<String> returnArgument = new ArrayList<String>(reply);

            DefaultNetconfProxyMessage replyMessage = new DefaultNetconfProxyMessage(
                    subjectType,
                    deviceId,
                    returnArgument,
                    localNodeId);

            unicastReplyToSender(replyMessage, proxyMessage.senderId());
            return reply;
        }

        @Override
        public Set<String> handleReplySetMessage(NetconfProxyMessage replyMessage) {
            replyArguments = new ArrayList<>(replyMessage.arguments());
            countDownLatch.countDown();

            return new LinkedHashSet<>(replyArguments);

        }
    }

    /**
     * Device factory for the specific NetconfDeviceImpl.
     *
     * @deprecated in 1.14.0
     */
    @Deprecated
    private class DefaultNetconfDeviceFactory implements NetconfDeviceFactory {

        @Override
        public NetconfDevice createNetconfDevice(NetconfDeviceInfo netconfDeviceInfo) throws NetconfException {
            return createNetconfDevice(netconfDeviceInfo, true);
        }

        @Beta
        @Override
        public NetconfDevice createNetconfDevice(NetconfDeviceInfo netconfDeviceInfo,
                                                 boolean isMaster)
                throws NetconfException {
            if (isMaster) {
                log.info("Creating NETCONF session to {} with {}",
                         netconfDeviceInfo.getDeviceId(), NetconfSshClientLib.APACHE_MINA);
            }
            return new DefaultNetconfDevice(netconfDeviceInfo, isMaster, NetconfControllerImpl.this);
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
                            for (NetconfDeviceListener l : netconfDeviceListeners) {
                                l.netconfConnectionReestablished(did);
                            }
                        } else {
                            log.warn("The device {} is not in the system", did);
                        }

                    } catch (NetconfException e) {
                        log.error("The SSH connection with device {} couldn't be " +
                                "reestablished due to {}. " +
                                "Marking the device as unreachable", did, e.getMessage());
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
