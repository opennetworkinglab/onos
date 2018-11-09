/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.provider.snmp.alarm.impl;

import com.google.common.collect.ImmutableList;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmProvider;
import org.onosproject.alarm.AlarmProviderRegistry;
import org.onosproject.alarm.AlarmProviderService;
import org.onosproject.alarm.DeviceAlarmConfig;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.snmp.SnmpController;
import org.onosproject.snmp.SnmpDevice;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which will listen for traps generated SNMP devices and configure
 * the devices with the Ip and Protocol fo the end point to send traps to.
 */
@Component(immediate = true)
public class SnmpAlarmProvider extends AbstractProvider
        implements AlarmProvider {

    public static final String COLON = ":";
    private final Logger log = getLogger(SnmpAlarmProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected SnmpController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected AlarmProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    private static final String APP_NAME = "org.onosproject.snmp";
    private static final String SCHEME = "snmp";

    protected AlarmProviderService providerService;

    protected ApplicationId appId;

    protected final DeviceListener deviceListener = new InternalDeviceListener();

    protected final MastershipListener mastershipListener = new InternalMastershipListener();

    protected final CommandResponder internalCommandResponder = new InternalCommandResponder();

    private IpAddress localIp;

    private final Map<Integer, Snmp> portSessionMap = new HashMap<>();

    private Snmp snmp;


    /**
     * Creates a provider with the supplier identifier.
     */
    public SnmpAlarmProvider() {
        super(new ProviderId("snmp", "org.onosproject.provider.alarm"));
    }

    @Activate
    public void activate(ComponentContext context) {

        providerService = providerRegistry.register(this);
        appId = coreService.registerApplication(APP_NAME);
        deviceService.addListener(deviceListener);
        mastershipService.addListener(mastershipListener);
        controller.getDevices().stream().forEach(d -> {
            triggerProbe(d.deviceId());
        });
        localIp = clusterService.getLocalNode().ip();
        log.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        for (Map.Entry<Integer, Snmp> entry : portSessionMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                log.warn("Can't close SNMP notification session on Ip {} and Port {}",
                         localIp, entry.getKey(), e);
            }
        }
        mastershipService.addListener(mastershipListener);
        deviceService.removeListener(deviceListener);
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Modified
    public void modified() {
        log.info("Modified");
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        requestTraps(deviceId);
        configureListeningConnection(controller.getDevice(deviceId));
    }

    private void configureListeningConnection(SnmpDevice device) {
        int notificationPort = device.getNotificationPort();
        if (notificationPort == 0 || portSessionMap.containsKey(notificationPort)) {
            return;
        }
        String protocol = device.getProtocol();
        try {
            Address listenAddress = GenericAddress.parse(protocol +
                                                         ":" + localIp.toString() +
                                                         "/" + notificationPort);
            TransportMapping tm;
            if (protocol == GenericAddress.TYPE_TCP) {
                tm = new DefaultTcpTransportMapping((TcpAddress) listenAddress);
            } else {
                tm = new DefaultUdpTransportMapping((UdpAddress) listenAddress);
            }
            snmp = new Snmp(tm);
            snmp.addCommandResponder(internalCommandResponder);
            snmp.listen();
            log.info("Initiated SNMP notification connection on Ip {} and Port {}",
                     localIp.toString(), notificationPort);
        } catch (IOException e) {
            log.error("Can't initiate SNMP transport on Protocol {}, Ip {} and Port {}",
                      protocol, localIp, notificationPort, e);
            return;
        }

        portSessionMap.put(notificationPort, snmp);
    }

    private boolean requestTraps(DeviceId deviceId) {
        SnmpDevice device = controller.getDevice(deviceId);
        DeviceAlarmConfig alarmTranslator = getAlarmTranslator(device);
        if (alarmTranslator != null) {
            return alarmTranslator.configureDevice(localIp, device.getNotificationPort(),
                                                   device.getNotificationProtocol());
        } else {
            log.warn("Device {} does not support alarms.", deviceId);
            return false;
        }
    }

    private DeviceAlarmConfig getAlarmTranslator(SnmpDevice device) {
        Driver deviceDriver = driverService.getDriver(device.deviceId());
        if (deviceDriver != null && deviceDriver.hasBehaviour(DeviceAlarmConfig.class)) {
            DriverData driverData = new DefaultDriverData(deviceDriver, device.deviceId());
            DeviceAlarmConfig alarmTranslator = deviceDriver
                    .createBehaviour(driverData, DeviceAlarmConfig.class);
            alarmTranslator.setHandler(new DefaultDriverHandler(driverData));
            return alarmTranslator;
        } else {
            log.warn("Device does not support alarm {}", device.deviceId());
        }
        return null;
    }

    private class InternalCommandResponder implements CommandResponder {
        @Override
        public void processPdu(CommandResponderEvent event) {
            try {
                log.debug("received trap {}", event);
                String[] deviceInfo = event.getPeerAddress().toString().split("/");

                //TODO This should be done via device service.
                //searching only for Ip since the port from which the trap is sent
                // could be different from the one used for SNMP
                SnmpDevice device = controller.getDevice(new URI(deviceInfo[0]));
                if (device != null) {
                    DeviceId deviceId = DeviceId.deviceId(SCHEME + COLON + deviceInfo[0]
                                                                  + COLON + device.getSnmpPort());
                    DeviceAlarmConfig alarmTranslator = getAlarmTranslator(controller.getDevice(deviceId));
                    if (alarmTranslator != null) {
                        Set<Alarm> alarms = alarmTranslator.translateAlarms(ImmutableList.of(event));
                        providerService.updateAlarmList(deviceId, alarms);
                    } else {
                        log.warn("Device {} does not support alarm", device.deviceId());
                    }
                } else {
                    log.error("Device {} does not exist in ONOS SNMP subsystem", deviceInfo[0]);
                }
                //Catching generic exception due to otherwise hidden URISyntax
            } catch (Exception e) {
                log.error("Exception while processing PDU {}", e);
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            DeviceId deviceId = event.subject().id();
            switch (event.type()) {
                case DEVICE_ADDED:
                    triggerProbe(deviceId);
                    break;
                case DEVICE_UPDATED:
                    triggerProbe(deviceId);
                    break;
                default:
                    break;
            }
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.subject().id().toString().startsWith(SCHEME)
                    && mastershipService.isLocalMaster(event.subject().id());
        }
    }

    private class InternalMastershipListener implements MastershipListener {

        @Override
        public void event(MastershipEvent event) {
            triggerProbe(event.subject());
        }

        @Override
        public boolean isRelevant(MastershipEvent event) {
            return event.subject().toString().startsWith(SCHEME)
                    && mastershipService.isLocalMaster(event.subject());
        }
    }
}
