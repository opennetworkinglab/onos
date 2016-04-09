/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.config;

import org.joda.time.LocalDateTime;
import org.onosproject.event.AbstractEvent;

import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes network configuration event.
 */
public class NetworkConfigEvent extends AbstractEvent<NetworkConfigEvent.Type, Object> {

    private final Class configClass;
    private final Config config;
    private final Config prevConfig;

    /**
     * Type of network configuration events.
     */
    public enum Type {
        /**
         * Signifies that a network configuration was registered.
         */
        CONFIG_REGISTERED,

        /**
         * Signifies that a network configuration was unregistered.
         */
        CONFIG_UNREGISTERED,

        /**
         * Signifies that network configuration was added.
         */
        CONFIG_ADDED,

        /**
         * Signifies that network configuration was updated.
         */
        CONFIG_UPDATED,

        /**
         * Signifies that network configuration was removed.
         */
        CONFIG_REMOVED
    }

    /**
     * Creates an event of a given type and for the specified subject and the
     * current time.
     *
     * @param type        event type
     * @param subject     event subject
     * @param configClass configuration class
     */
    public NetworkConfigEvent(Type type, Object subject, Class configClass) {
        super(type, subject);
        this.configClass = configClass;
        this.config = null;
        this.prevConfig = null;
    }

    /**
     * Creates an event of a given type and for the specified subject and time.
     *
     * @param type        device event type
     * @param subject     event subject
     * @param configClass configuration class
     * @param time        occurrence time
     */
    public NetworkConfigEvent(Type type, Object subject, Class configClass, long time) {
        super(type, subject, time);
        this.configClass = configClass;
        this.config = null;
        this.prevConfig = null;
    }

    /**
     * Creates an event of a given type and for the specified subject,
     * previous config and time.
     *
     * @param type        device event type
     * @param subject     event subject
     * @param configClass configuration class
     * @param config      current config
     * @param prevConfig  previous config
     */
    public NetworkConfigEvent(Type type, Object subject, Config config,
            Config prevConfig, Class configClass) {
        super(type, subject);
        this.configClass = configClass;
        this.config = config;
        this.prevConfig = prevConfig;
    }

    /**
     * Returns the class of configuration that has been changed.
     *
     * @return configuration class
     */
    public Class configClass() {
        return configClass;
    }

    /**
     * Returns current config.
     *
     * @return current config; value presents only when the type is
     *         CONFIG_ADDED or CONFIG_UPDATED
     */
    public Optional<Config> config() {
        return (config != null) ? Optional.of(config) : Optional.empty();
    }

    /**
     * Returns previous config.
     *
     * @return previous config; value presents only when the type is
     *         CONFIG_UPDATED or CONFIG_REMOVED
     */
    public Optional<Config> prevConfig() {
        return (prevConfig != null) ? Optional.of(prevConfig) : Optional.empty();
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", new LocalDateTime(time()))
                .add("type", type())
                .add("config", config())
                .add("prevConfig", prevConfig())
                .add("configClass", configClass())
                .toString();
    }
}
