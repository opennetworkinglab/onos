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

/**
 * Base abstract factory for creating configurations for the specified subject type.
 *
 * @param <S> type of subject
 * @param <C> type of configuration
 */
@Beta
public abstract class ConfigFactory<S, C extends Config<S>> {

    private final SubjectFactory<S> subjectFactory;
    private final Class<C> configClass;
    private final String configKey;

    /**
     * Creates a new configuration factory for the specified class of subjects
     * capable of generating the configurations of the specified class. The
     * subject and configuration class keys are used merely as keys for use in
     * composite JSON trees.
     *
     * @param subjectFactory subject factory
     * @param configClass  configuration class
     * @param configKey    configuration class key
     */
    protected ConfigFactory(SubjectFactory<S> subjectFactory,
                            Class<C> configClass, String configKey) {
        this.subjectFactory = subjectFactory;
        this.configClass = configClass;
        this.configKey = configKey;
    }

    /**
     * Returns the class of the subject to which this factory applies.
     *
     * @return subject type
     */
    public SubjectFactory<S> subjectFactory() {
        return subjectFactory;
    }

    /**
     * Returns the class of the configuration which this factory generates.
     *
     * @return configuration type
     */
    public Class<C> configClass() {
        return configClass;
    }

    /**
     * Returns the unique key (within subject class) of this configuration.
     * This is primarily aimed for use in composite JSON trees in external
     * representations and has no bearing on the internal behaviours.
     *
     * @return configuration key
     */
    public String configKey() {
        return configKey;
    }

    /**
     * Creates a new but uninitialized configuration. Framework will initialize
     * the configuration via {@link Config#init} method.
     *
     * @return new uninitialized configuration
     */
    public abstract C createConfig();

}
