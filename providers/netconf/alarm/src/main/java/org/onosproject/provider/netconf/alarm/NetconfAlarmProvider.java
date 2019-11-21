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

package org.onosproject.provider.netconf.alarm;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.onosproject.netconf.NetconfException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmProvider;
import org.onosproject.alarm.AlarmProviderService;
import org.onosproject.alarm.AlarmProviderRegistry;
import org.onosproject.alarm.AlarmTranslator;
import org.onosproject.alarm.DeviceAlarmConfig;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.netconf.FilteringNetconfDeviceOutputEventListener;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceListener;
import org.onosproject.netconf.NetconfDeviceOutputEvent;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an Alarm Manager to keep track of device notifications.
 */
@Component(immediate = true, service = AlarmProvider.class)
public class NetconfAlarmProvider extends AbstractProvider implements AlarmProvider {

    public static final String ACTIVE = "active";
    private final Logger log = getLogger(getClass());
    private final AlarmTranslator translator = new NetconfAlarmTranslator();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected AlarmProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetconfController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    protected AlarmProviderService providerService;

    private Map<DeviceId, InternalNotificationListener> idNotificationListenerMap = Maps.newHashMap();

    public NetconfAlarmProvider() {
        super(new ProviderId("netconf", "org.onosproject.netconf"));
    }

    private NetconfDeviceListener deviceListener = new InnerDeviceListener();

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.getNetconfDevices().forEach(id -> {
            NetconfDevice device = controller.getNetconfDevice(id);
            if (device.isMasterSession()) {
                NetconfSession session = device.getSession();
                InternalNotificationListener listener = new InternalNotificationListener(device.getDeviceInfo());
                try {
                    session.addDeviceOutputListener(listener);
                } catch (NetconfException e) {
                    log.error("addDeviceOutputListener Error {} ", e.getMessage());
                }
                idNotificationListenerMap.put(id, listener);
            }
        });
        controller.addDeviceListener(deviceListener);
        log.info("NetconfAlarmProvider Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        idNotificationListenerMap.forEach((id, listener) -> {
            NetconfDevice device = controller.getNetconfDevice(id);
            if (device.isMasterSession()) {
                try {
                    device.getSession().removeDeviceOutputListener(listener);
                } catch (NetconfException e) {
                    log.error("RemoveDeviceOutputListener Error {}", e.getMessage());
                }
            }
        });
        controller.removeDeviceListener(deviceListener);
        providerService = null;
        log.info("NetconfAlarmProvider Stopped");
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        log.debug("Alarm probe triggered with {}", deviceId);
    }

    private void triggerProbe(DeviceId deviceId, Collection<Alarm> alarms) {
        providerService.updateAlarmList(deviceId, alarms);
        triggerProbe(deviceId);
    }

    private class InternalNotificationListener
            extends FilteringNetconfDeviceOutputEventListener
            implements NetconfDeviceOutputEventListener {

        InternalNotificationListener(NetconfDeviceInfo deviceInfo) {
            super(deviceInfo);
        }

        @Override
        public void event(NetconfDeviceOutputEvent event) {
            if (event.type() == NetconfDeviceOutputEvent.Type.DEVICE_NOTIFICATION) {
                DeviceId deviceId = event.getDeviceInfo().getDeviceId();
                Driver deviceDriver = driverService.getDriver(deviceId);
                Device device = deviceService.getDevice(deviceId);
                if (deviceDriver != null && device.is(DeviceAlarmConfig.class)) {
                    DeviceAlarmConfig alarmTranslator = device.as(DeviceAlarmConfig.class);
                    Set<Alarm> alarms = alarmTranslator.translateAlarms(ImmutableList.of(event));
                    triggerProbe(deviceId, alarms);
                } else {
                    String message = event.getMessagePayload();
                    InputStream in = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
                    Collection<Alarm> newAlarms = translator.translateToAlarm(deviceId, in);
                    triggerProbe(deviceId, newAlarms);
                }
            }
        }
    }

    private class InnerDeviceListener implements NetconfDeviceListener {

        @Override
        public void deviceAdded(DeviceId deviceId) {
            try {
                NetconfDevice device = controller.getNetconfDevice(deviceId);
                NetconfSession session = device.getSession();
                InternalNotificationListener listener = new InternalNotificationListener(device.getDeviceInfo());
                session.addDeviceOutputListener(listener);
                idNotificationListenerMap.put(deviceId, listener);
            } catch (NetconfException e) {
                log.error("Device add fail {}", e.getMessage());
            }
        }

        @Override
        public void deviceRemoved(DeviceId deviceId) {
            idNotificationListenerMap.remove(deviceId);
        }
    }
}
