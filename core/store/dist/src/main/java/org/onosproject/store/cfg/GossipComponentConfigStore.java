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
package org.onosproject.store.cfg;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cfg.ComponentConfigEvent;
import org.onosproject.cfg.ComponentConfigStore;
import org.onosproject.cfg.ComponentConfigStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import static org.onosproject.cfg.ComponentConfigEvent.Type.PROPERTY_SET;
import static org.onosproject.cfg.ComponentConfigEvent.Type.PROPERTY_UNSET;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.PUT;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.REMOVE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of component configurations in a distributed data store
 * that uses optimistic replication and gossip based anti-entropy techniques.
 */
@Component(immediate = true)
@Service
public class GossipComponentConfigStore
        extends AbstractStore<ComponentConfigEvent, ComponentConfigStoreDelegate>
        implements ComponentConfigStore {

    private static final String SEP = "#";

    private final Logger log = getLogger(getClass());

    private EventuallyConsistentMap<String, String> properties;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    @Activate
    public void activate() {
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API);

        properties = storageService.<String, String>eventuallyConsistentMapBuilder()
                .withName("cfg")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        properties.addListener(new InternalPropertiesListener());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        properties.destroy();
        log.info("Stopped");
    }

    @Override
    public void setProperty(String componentName, String name, String value) {
        properties.put(key(componentName, name), value);

    }

    @Override
    public void unsetProperty(String componentName, String name) {
        properties.remove(key(componentName, name));
    }

    /**
     * Listener to component configuration properties distributed map changes.
     */
    private final class InternalPropertiesListener
            implements EventuallyConsistentMapListener<String, String> {

        @Override
        public void event(EventuallyConsistentMapEvent<String, String> event) {
            String[] keys = event.key().split(SEP);
            String value = event.value();
            if (event.type() == PUT) {
                delegate.notify(new ComponentConfigEvent(PROPERTY_SET, keys[0], keys[1], value));
            } else if (event.type() == REMOVE) {
                delegate.notify(new ComponentConfigEvent(PROPERTY_UNSET, keys[0], keys[1], null));
            }
        }
    }

    // Generates a key from component name and property name.
    private String key(String componentName, String name) {
        return componentName + SEP + name;
    }

}
