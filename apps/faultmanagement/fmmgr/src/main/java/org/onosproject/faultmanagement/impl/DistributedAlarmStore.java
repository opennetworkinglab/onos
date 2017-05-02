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

package org.onosproject.faultmanagement.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.faultmanagement.api.AlarmStore;
import org.onosproject.faultmanagement.api.AlarmStoreDelegate;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEntityId;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEvent;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmId;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages information of alarms using gossip protocol to distribute
 * information.
 */
@Component(immediate = true)
@Service
public class DistributedAlarmStore
        extends AbstractStore<AlarmEvent, AlarmStoreDelegate>
        implements AlarmStore {

    private final Logger log = getLogger(getClass());
    private ConsistentMap<AlarmId, Alarm> alarms;
    private Map<AlarmId, Alarm> alarmsMap;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private final MapEventListener<AlarmId, Alarm> listener = new InternalListener();

    @Activate
    public void activate() {
        log.info("Started");
        alarms = storageService.<AlarmId, Alarm>consistentMapBuilder()
                .withName("onos-alarm-table")
                .withSerializer(Serializer.using(KryoNamespaces.API,
                                                 Alarm.class,
                                                 DefaultAlarm.class,
                                                 AlarmId.class,
                                                 AlarmEvent.Type.class,
                                                 Alarm.SeverityLevel.class,
                                                 AlarmEntityId.class))
                .build();
        alarms.addListener(listener);
        alarmsMap = alarms.asJavaMap();
    }

    @Deactivate
    public void deactivate() {
        alarms.removeListener(listener);
        log.info("Stopped");
    }

    @Modified
    public boolean modified() {
        log.info("Modified");
        return true;
    }

    @Override
    public Alarm getAlarm(AlarmId alarmId) {
        return alarmsMap.get(alarmId);
    }

    @Override
    public Collection<Alarm> getAlarms() {
        return ImmutableSet.copyOf(alarmsMap.values());
    }

    @Override
    public Collection<Alarm> getAlarms(DeviceId deviceId) {
        //FIXME: this is expensive, need refactoring when core maps provide different indexes.
        return ImmutableSet.copyOf(alarmsMap.values().stream()
                                           .filter(alarm -> alarm.deviceId().equals(deviceId))
                                           .collect(Collectors.toSet()));
    }

    @Override
    public void createOrUpdateAlarm(Alarm alarm) {
        Alarm existing = alarmsMap.get(alarm.id());
        if (Objects.equals(existing, alarm)) {
            log.info("Received identical alarm, no operation needed on {}", alarm.id());
        } else {
            alarms.put(alarm.id(), alarm);
        }
    }

    @Override
    public void removeAlarm(AlarmId alarmId) {
        alarms.remove(alarmId);
    }

    //Event listener to notify delegates about Map events.
    private class InternalListener implements MapEventListener<AlarmId, Alarm> {

        @Override
        public void event(MapEvent<AlarmId, Alarm> mapEvent) {
            final AlarmEvent.Type type;
            final Alarm alarm;
            switch (mapEvent.type()) {
                case INSERT:
                    type = AlarmEvent.Type.CREATED;
                    alarm = mapEvent.newValue().value();
                    break;
                case UPDATE:
                    type = AlarmEvent.Type.UPDATED;
                    alarm = mapEvent.newValue().value();
                    break;
                case REMOVE:
                    type = AlarmEvent.Type.REMOVED;
                    alarm = mapEvent.oldValue().value();
                    break;
                default:
                    throw new IllegalArgumentException("Wrong event type " + mapEvent.type());
            }
            notifyDelegate(new AlarmEvent(type, alarm));
        }
    }
}
