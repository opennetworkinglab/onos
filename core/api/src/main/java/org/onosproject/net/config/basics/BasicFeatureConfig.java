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
package org.onosproject.net.config.basics;

import org.onosproject.net.config.Config;

/**
 * Base abstraction for configuring feature on subject.
 *
 * @param <S> Subject type
 */
public abstract class BasicFeatureConfig<S> extends Config<S> {

    private static final String ENABLED = "enabled";

    private final boolean defaultValue;

    protected BasicFeatureConfig(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Indicates whether the feature for the subject is enabled.
     *
     * @return true if feature is enabled
     */
    public boolean enabled() {
        return get(ENABLED, defaultValue);
    }

    /**
     * Specifies whether the feature for the subject is to be enabled.
     *
     * @param enabled true to enable; false to disable; null to clear
     * @return self
     */
    public BasicFeatureConfig<S> enabled(Boolean enabled) {
        return (BasicFeatureConfig<S>) setOrClear(ENABLED, enabled);
    }

}
