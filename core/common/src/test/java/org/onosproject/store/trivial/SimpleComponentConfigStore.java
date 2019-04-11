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
package org.onosproject.store.trivial;

import org.onosproject.cfg.ComponentConfigEvent;
import org.onosproject.cfg.ComponentConfigStore;
import org.onosproject.cfg.ComponentConfigStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import static org.onosproject.cfg.ComponentConfigEvent.Type.PROPERTY_SET;
import static org.onosproject.cfg.ComponentConfigEvent.Type.PROPERTY_UNSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of component configuration properties.
 */
@Component(immediate = true, service = ComponentConfigStore.class)
public class SimpleComponentConfigStore
        extends AbstractStore<ComponentConfigEvent, ComponentConfigStoreDelegate>
        implements ComponentConfigStore {

    private final Logger log = getLogger(getClass());

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void setProperty(String componentName, String name, String value) {
        delegate.notify(new ComponentConfigEvent(PROPERTY_SET, componentName, name, value));
    }

    @Override
    public void setProperty(String componentName, String name, String value, boolean override) {
        setProperty(componentName, name, value);
    }

    @Override
    public void unsetProperty(String componentName, String name) {
        delegate.notify(new ComponentConfigEvent(PROPERTY_UNSET, componentName, name, null));
    }
}
