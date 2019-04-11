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
package org.onosproject.store.cfg;

import com.google.common.collect.ImmutableSet;
import org.onosproject.cfg.ComponentConfigEvent;
import org.onosproject.cfg.ComponentConfigStore;
import org.onosproject.cfg.ComponentConfigStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;

import static org.onosproject.cfg.ComponentConfigEvent.Type.PROPERTY_SET;
import static org.onosproject.cfg.ComponentConfigEvent.Type.PROPERTY_UNSET;
import static org.onosproject.store.service.MapEvent.Type.INSERT;
import static org.onosproject.store.service.MapEvent.Type.REMOVE;
import static org.onosproject.store.service.MapEvent.Type.UPDATE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of component configurations in a distributed data store
 * that provides strong sequential consistency guarantees.
 */
@Component(immediate = true, service = ComponentConfigStore.class)
public class DistributedComponentConfigStore
        extends AbstractStore<ComponentConfigEvent, ComponentConfigStoreDelegate>
        implements ComponentConfigStore {

    private static final String SEP = "#";

    private final Logger log = getLogger(getClass());

    private ConsistentMap<String, String> properties;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private InternalPropertiesListener propertiesListener = new InternalPropertiesListener();

    @Activate
    public void activate() {
        properties = storageService.<String, String>consistentMapBuilder()
                .withName("onos-component-cfg")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .withRelaxedReadConsistency()
                .build();

        properties.addListener(propertiesListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        properties.removeListener(propertiesListener);
        log.info("Stopped");
    }

    @Override
    public void setProperty(String componentName, String name, String value) {
        setProperty(componentName, name, value, true);
    }

    @Override
    public void setProperty(String componentName, String name, String value, boolean override) {
        properties.compute(key(componentName, name), (k, v) -> (override || v == null) ? value : v);
    }

    @Override
    public void unsetProperty(String componentName, String name) {
        properties.remove(key(componentName, name));
    }

    @Override
    public Set<String> getProperties(String componentName) {
        ImmutableSet.Builder<String> names = ImmutableSet.builder();
        properties.keySet().stream()
                .filter((String k) -> Objects.equals(componentName, k.substring(0, k.indexOf(SEP))))
                .map((String k) -> k.substring(k.indexOf(SEP) + 1))
                .forEach(names::add);
        return names.build();
    }

    @Override
    public String getProperty(String componentName, String name) {
        Versioned<String> v = properties.get(key(componentName, name));
        return v != null ? v.value() : null;
    }

    /**
     * Listener to component configuration properties distributed map changes.
     */
    private final class InternalPropertiesListener
            implements MapEventListener<String, String> {

        @Override
        public void event(MapEvent<String, String> event) {
            String[] keys = event.key().split(SEP);
            if (event.type() == INSERT || event.type() == UPDATE) {
                String value = event.newValue().value();
                notifyDelegate(new ComponentConfigEvent(PROPERTY_SET, keys[0], keys[1], value));
            } else if (event.type() == REMOVE) {
                notifyDelegate(new ComponentConfigEvent(PROPERTY_UNSET, keys[0], keys[1], null));
            }
        }
    }

    // Generates a key from component name and property name.
    private String key(String componentName, String name) {
        return componentName + SEP + name;
    }
}
