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
package org.onosproject.net.config;

import com.google.common.annotations.Beta;

import java.util.Set;

/**
 * Service for tracking network configuration factories. It is the basis for
 * extensibility to allow various core subsystems or apps to register their
 * own configuration factories that permit use to inject additional meta
 * information about how various parts of the network should be viewed and
 * treated.
 */
@Beta
public interface NetworkConfigRegistry extends NetworkConfigService {

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
     * Returns set of all registered configuration factories.
     *
     * @return set of config factories
     */
    Set<ConfigFactory> getConfigFactories();

    /**
     * Returns set of all configuration factories registered for the specified
     * class of subject.
     *
     * @param subjectClass subject class
     * @param <S>          type of subject
     * @param <C>          type of configuration
     * @return set of config factories
     */
    <S, C extends Config<S>> Set<ConfigFactory<S, C>> getConfigFactories(Class<S> subjectClass);

    /**
     * Returns the configuration factory that produces the specified class of
     * configurations.
     *
     * @param configClass configuration class
     * @param <S>         type of subject
     * @param <C>         type of configuration
     * @return config factory
     */
    <S, C extends Config<S>> ConfigFactory<S, C> getConfigFactory(Class<C> configClass);

}
