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
package org.onosproject.odtn.utils.openconfig;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.yang.gen.v1.openconfigplatform.rev20161222.openconfigplatform.platformcomponenttop.components.Component;
import org.onosproject.yang.gen.v1.openconfigplatform.rev20161222.openconfigplatform.platformcomponenttop.components.DefaultComponent;
import org.onosproject.yang.gen.v1.openconfigplatform.rev20161222.openconfigplatform.platformcomponenttop.components.component.Config;
import org.onosproject.yang.gen.v1.openconfigplatform.rev20161222.openconfigplatform.platformcomponenttop.components.component.DefaultConfig;

import com.google.common.annotations.Beta;

/**
 * Utility methods dealing with OpenConfig component.
 * <p>
 * Split into classes for the purpose of avoiding "Config" class collisions.
 */
@Beta
public abstract class PlainPlatform {

    public static Component componentWithName(String componentName) {
        checkNotNull(componentName, "componentName cannot be null");

        Component component = new DefaultComponent();
        component.name(componentName);
        Config config = new DefaultConfig();
        config.name(componentName);
        component.config(config);
        return component;
    }
}
