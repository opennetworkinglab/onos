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
package org.onosproject.provider.snmp.alarm.impl;

import com.btisystems.pronx.ems.core.snmp.DefaultSnmpConfigurationFactory;
import com.btisystems.pronx.ems.core.snmp.ISnmpConfiguration;
import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import com.btisystems.pronx.ems.core.snmp.ISnmpSessionFactory;
import com.btisystems.pronx.ems.core.snmp.SnmpSessionFactory;
import com.btisystems.pronx.ems.core.snmp.V2cSnmpConfiguration;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEvent;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmListener;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmProvider;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * SNMP alarms provider.
 * @deprecated 1.5.0 Falcon, not compliant with ONOS SB and driver architecture.
 */
@Deprecated
@Component(immediate = true)
@Service
public class SnmpAlarmProviderService extends AbstractProvider implements AlarmProvider {

    private final Logger log = getLogger(getClass());

    private final InternalDeviceListener internalDeviceListener = new InternalDeviceListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private ApplicationId appId;

    private final ISnmpSessionFactory sessionFactory;

    // TODO convert to standard ONOS listener service approach  ?
    protected Set<AlarmListener> alarmEventListener = Sets.newHashSet();

    private ExecutorService eventHandlingExecutor;

    // TODO Could be replaced with a service lookup, and bundles per device variant.
    Map<String, SnmpDeviceAlarmProvider> providers = new HashMap<>();

    @Deprecated
    public SnmpAlarmProviderService() {
        super(new ProviderId("snmp", "org.onosproject.provider.alarm"));
        log.info("SnmpAlarmProviderService ...");
        sessionFactory = new SnmpSessionFactory(
                new DefaultSnmpConfigurationFactory(new V2cSnmpConfiguration()));
        providers.put("1.3.6.1.4.1.18070.2.2", new Bti7000SnmpAlarmProvider());
        providers.put("1.3.6.1.4.1.20408", new NetSnmpAlarmProvider());
    }

    @Activate
    public void activate(ComponentContext context) {
        appId = coreService.registerApplication("org.onosproject.snmp");
        eventHandlingExecutor = Executors.newSingleThreadExecutor(
                groupedThreads("onos/alarms", "event-handler"));
        deviceService.addListener(internalDeviceListener);
        log.info("activated SNMP provider with appId = {} and context props {}", appId, context.getProperties());
        modified(context);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("deactivate SNMP provider {}", appId);
        deviceService.removeListener(internalDeviceListener);
    }

    @Modified
    public void modified(ComponentContext context) {
        log.info("modified {}", context);

        if (context == null) {
            log.info("No configuration file");
        }

    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        log.info("SNMP walk request for alarms at deviceId={}", deviceId);
        if (!isSnmpDevice(deviceId)) {
            log.info("Ignore non-snmp device!");
            return;
        }
        String[] deviceComponents = deviceId.toString().split(":");
        Set<Alarm> alarms = new HashSet<>(Sets.newHashSet());

        if (deviceComponents.length > 1) {
            String ipAddress = deviceComponents[1];
            String port = deviceComponents[2];
            ISnmpConfiguration config = new V2cSnmpConfiguration();
            config.setPort(Integer.parseInt(port));

            try (ISnmpSession session = getSessionFactory().createSession(config, ipAddress)) {
                // Each session will be auto-closed.
                String deviceOid = session.identifyDevice();
                alarms.addAll(getAlarmsForDevice(deviceOid, session, deviceId));
                log.info("SNMP walk completed ok for deviceId={}", deviceId);
            } catch (IOException | RuntimeException ex) {
                log.error("Failed to walk device.", ex.getMessage());
                log.debug("Detailed problem was ", ex);
                alarms.add(
                        buildWalkFailedAlarm(deviceId)
                );
            }
        }

        AlarmEvent alarmEvent = new AlarmEvent(alarms, deviceId);

        alarmEventListener.stream().forEach((listener) -> {
            listener.event(alarmEvent);
            log.info("Successfully event with discovered alarms for deviceId={} to {}", deviceId, listener);
        });

    }

    private static DefaultAlarm buildWalkFailedAlarm(DeviceId deviceId) {
        return new DefaultAlarm.Builder(
                deviceId, "SNMP alarm retrieval failed",
                Alarm.SeverityLevel.CRITICAL,
                System.currentTimeMillis()).build();
    }

    protected ISnmpSessionFactory getSessionFactory() {
        return sessionFactory;
    }

    private Collection<Alarm> getAlarmsForDevice(String deviceOid, ISnmpSession session,
            DeviceId deviceID) throws IOException {
        Collection<Alarm> alarms = new HashSet<>();
        if (providers.containsKey(deviceOid)) {
            alarms.addAll(providers.get(deviceOid).getAlarms(session, deviceID));
        }
        return alarms;
    }

    @Override
    public void addAlarmListener(AlarmListener listener) {
        alarmEventListener.add(checkNotNull(listener, "Listener cannot be null"));
    }

    @Override
    public void removeAlarmListener(AlarmListener listener) {
        alarmEventListener.remove(checkNotNull(listener, "Listener cannot be null"));
    }

    /**
     * Internal listener for device service events.
     */
    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            log.info("InternalDeviceListener has got event from device-service{} with ", event);
            eventHandlingExecutor.execute(() -> {
                try {
                    DeviceId deviceId = event.subject().id();
                    log.info("From device {}", deviceId);
                    if (!isSnmpDevice(deviceId)) {
                        log.info("Ignore non-snmp device event for {}", deviceId);
                        return;
                    }

                    switch (event.type()) {
                        case DEVICE_ADDED:
                        case DEVICE_UPDATED:
                        case DEVICE_AVAILABILITY_CHANGED:
                            if (deviceService.isAvailable(event.subject().id())) {
                                triggerProbe(deviceId);
                            }
                            break;
                        case DEVICE_REMOVED:
                        case DEVICE_SUSPENDED:
                        default:
                            // Could potentially remove all alarms when eg DEVICE_REMOVED or DEVICE_SUSPENDED
                            // however for now ignore and fall through
                            break;
                    }
                } catch (Exception e) {
                    log.warn("Failed to process {}", event, e);
                }
            });
        }

    }

    private static boolean isSnmpDevice(DeviceId deviceId) {
        return deviceId.uri().getScheme().equalsIgnoreCase("snmp");
    }
}
