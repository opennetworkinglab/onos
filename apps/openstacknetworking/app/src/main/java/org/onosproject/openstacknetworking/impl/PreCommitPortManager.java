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
package org.onosproject.openstacknetworking.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type;
import org.onosproject.openstacknetworking.api.PreCommitPortService;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of pre-commit service.
 */
@Service
@Component(immediate = true)
public class PreCommitPortManager implements PreCommitPortService {

    protected final Logger log = getLogger(getClass());

    private Map<String, Map<Type, Set<String>>> store = Maps.newConcurrentMap();

    @Activate
    protected void activate() {
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void subscribePreCommit(String portId, Type eventType, String className) {
        store.computeIfAbsent(portId, s -> Maps.newConcurrentMap());

        store.compute(portId, (k, v) -> {

            if (className == null || className.isEmpty()) {
                return null;
            }

            Objects.requireNonNull(v).computeIfAbsent(eventType,
                                     s -> Sets.newConcurrentHashSet());


            Objects.requireNonNull(v).compute(eventType, (i, j) -> {
                Objects.requireNonNull(j).add(className);
                return j;
            });

            return v;
        });
    }

    @Override
    public void unsubscribePreCommit(String portId, Type eventType, String className) {

        store.computeIfPresent(portId, (k, v) -> {

            if (className == null || className.isEmpty()) {
                return null;
            }

            Objects.requireNonNull(v).computeIfPresent(eventType, (i, j) -> {
                Objects.requireNonNull(j).remove(className);
                return j;
            });

            return v;
        });
    }

    @Override
    public int subscriberCountByEventType(String portId, Type eventType) {

        Map<Type, Set<String>> typeMap = store.get(portId);

        if (typeMap == null || typeMap.isEmpty()) {
            return 0;
        }

        if (typeMap.get(eventType) == null || typeMap.get(eventType).isEmpty()) {
            return 0;
        }

        return typeMap.get(eventType).size();
    }

    @Override
    public int subscriberCount(String portId) {

        Map<Type, Set<String>> typeMap = store.get(portId);

        if (typeMap == null || typeMap.isEmpty()) {
            return 0;
        }

        return typeMap.values().stream()
                .filter(Objects::nonNull)
                .map(Set::size)
                .reduce(0, Integer::sum);
    }
}
