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
package org.onosproject.incubator.net.config;

import com.google.common.annotations.Beta;

import java.util.Set;

/**
 * Service for tracking network configuration factories.
 */
@Beta
public interface NetworkConfigRegistry {

    /**
     * Registers the specified configuration factory.
     *
     * @param configFactory configuration factory
     */
    void registerConfigFactory(ConfigFactory configFactory);

    /**
     * Unregisters the specified configuration factory.
     *
     * @param configFactory configuration factory
     */
    void unregisterConfigFactory(ConfigFactory configFactory);

    /**
     * Returns set of configuration factories available for the specified
     * class of subject.
     *
     * @param subjectClass subject class
     * @param <T> type of subject
     * @return set of config factories
     */
    <T> Set<ConfigFactory<T>> getConfigFactories(Class<T> subjectClass);

    /**
     * Returns the configuration type registered for the specified
     * subject type and key.
     *
     * @param subjectClass subject class
     * @param configKey    configuration key
     * @param <T> type of subject
     * @return config factory
     */
    <T> ConfigFactory<T> getConfigFactory(Class<T> subjectClass, String configKey);

    /**
     * Returns the configuration type registered for the specified
     * configuration class.
     *
     * @param configClass configuration class
     * @param <T> type of subject
     * @return config factory
     */
    <T> ConfigFactory<T> getConfigFactory(Class<Config<T>> configClass);

}
