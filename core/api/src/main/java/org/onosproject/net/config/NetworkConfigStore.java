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
package org.onosproject.net.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Mechanism for distributing and storing network configuration information.
 */
public interface NetworkConfigStore extends Store<NetworkConfigEvent, NetworkConfigStoreDelegate> {

    /**
     * Adds a new configuration factory.
     *
     * @param configFactory configuration factory to add
     */
    void addConfigFactory(ConfigFactory configFactory);

    /**
     * Removes a configuration factory.
     *
     * @param configFactory configuration factory to remove
     */
    void removeConfigFactory(ConfigFactory configFactory);

    /**
     * Returns the configuration factory for the specified configuration class.
     *
     * @param configClass configuration class
     * @param <S>         type of subject
     * @param <C>         type of configuration
     * @return configuration factory or null
     */
    <S, C extends Config<S>> ConfigFactory<S, C> getConfigFactory(Class<C> configClass);

    /**
     * Returns set of subjects of the specified class, which have some
     * network configuration associated with them.
     *
     * @param subjectClass subject class
     * @param <S>          type of subject
     * @return set of subject
     */
    <S> Set<S> getSubjects(Class<S> subjectClass);

    /**
     * Returns set of subjects of the specified class, which have the
     * specified class of network configuration associated with them.
     *
     * @param subjectClass subject class
     * @param configClass  configuration class
     * @param <S>          type of subject
     * @param <C>          type of configuration
     * @return set of subject
     */
    <S, C extends Config<S>> Set<S> getSubjects(Class<S> subjectClass, Class<C> configClass);

    /**
     * Returns set of configuration classes available for the specified subject.
     *
     * @param subject configuration subject
     * @param <S>     type of subject
     * @return set of configuration classes
     */
    <S> Set<Class<? extends Config<S>>> getConfigClasses(S subject);

    /**
     * Get the configuration of the given class and for the specified subject.
     *
     * @param subject     configuration subject
     * @param configClass configuration class
     * @param <S>         type of subject
     * @param <C>         type of configuration
     * @return configuration object
     */
    <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass);

    /**
     * Creates a new configuration of the given class for the specified subject.
     *
     * @param subject     configuration subject
     * @param configClass configuration class
     * @param <S>         type of subject
     * @param <C>         type of configuration
     * @return configuration object
     */
    <S, C extends Config<S>> C createConfig(S subject, Class<C> configClass);

    /**
     * Applies configuration for the specified subject and configuration
     * class using the raw JSON object. If configuration already exists, it
     * will be updated.
     *
     * @param subject     configuration subject
     * @param configClass configuration class
     * @param json        raw JSON node containing the configuration data
     * @param <S>         type of subject
     * @param <C>         type of configuration
     * @return configuration object
     */
    <S, C extends Config<S>> C applyConfig(S subject, Class<C> configClass,
                                           JsonNode json);

    /**
     * Clears the configuration of the given class for the specified subject.
     *
     * @param subject     configuration subject
     * @param configClass configuration class
     * @param <S>         type of subject
     * @param <C>         type of configuration
     */
    <S, C extends Config<S>> void clearConfig(S subject, Class<C> configClass);

}
