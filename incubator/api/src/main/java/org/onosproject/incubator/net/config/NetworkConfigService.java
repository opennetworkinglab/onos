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
 * Service for tracking network configurations which specify how the discovered
 * network information should be interpreted and how the network should be
 * configured.
 */
@Beta
public interface NetworkConfigService {

    /**
     * Returns the set of subjects for which some configuration is available.
     *
     * @param subjectClass subject class
     * @param <T> type of subject
     * @return set of configured subjects
     */
    <T> Set<T> getSubjects(Class<T> subjectClass);

    /**
     * Returns the set of subjects for which the specified configuration is
     * available.
     *
     * @param subjectClass subject class
     * @param configClass  configuration class
     * @param <T> type of subject
     * @return set of configured subjects
     */
    <T> Set<T> getSubjects(Class<T> subjectClass, Class<Config<T>> configClass);


    /**
     * Returns all configurations for the specified subject.
     *
     * @param subject configuration subject
     * @param <T> type of subject
     * @return set of configurations
     */
    <T> Set<Config<T>> getConfigs(T subject);

    /**
     * Returns the configuration for the specified subject and configuration
     * class if one is available; null otherwise.
     *
     * @param subject     configuration subject
     * @param configClass configuration class
     * @param <T> type of subject
     * @return configuration or null if one is not available
     */
    <T> Config<T> getConfig(T subject, Class<Config<T>> configClass);

}
