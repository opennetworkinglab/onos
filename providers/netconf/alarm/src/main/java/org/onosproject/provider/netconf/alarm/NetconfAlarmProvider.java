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

package org.onosproject.provider.netconf.alarm;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmProvider;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmProviderService;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmProviderRegistry;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmTranslator;
import org.onosproject.net.DeviceId;
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

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an Alarm Manager to keep track of device notifications.
 */
@Component(immediate = true)
public class NetconfAlarmProvider extends AbstractProvider implements AlarmProvider {

    public static final String ACTIVE = "active";
    private final Logger log = getLogger(getClass());
    private final AlarmTranslator translator = new NetconfAlarmTranslator();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected AlarmProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetconfController controller;

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
            NetconfSession session = device.getSession();
            InternalNotificationListener listener = new InternalNotificationListener(device.getDeviceInfo());
            session.addDeviceOutputListener(listener);
            idNotificationListenerMap.put(id, listener);
        });
        controller.addDeviceListener(deviceListener);
        log.info("NetconfAlarmProvider Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        idNotificationListenerMap.forEach((id, listener) -> {
            controller.getNetconfDevice(id)
                    .getSession()
                    .removeDeviceOutputListener(listener);
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
                String message = event.getMessagePayload();
                InputStream in = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
                Collection<Alarm> newAlarms = translator.translateToAlarm(deviceId, in);
                triggerProbe(deviceId, newAlarms);
            }
        }
    }

    private class InnerDeviceListener implements NetconfDeviceListener {

        @Override
        public void deviceAdded(DeviceId deviceId) {
            NetconfDevice device = controller.getNetconfDevice(deviceId);
            NetconfSession session = device.getSession();
            InternalNotificationListener listener = new InternalNotificationListener(device.getDeviceInfo());
            session.addDeviceOutputListener(listener);
            idNotificationListenerMap.put(deviceId, listener);
        }

        @Override
        public void deviceRemoved(DeviceId deviceId) {
            idNotificationListenerMap.remove(deviceId);
        }
    }
}
