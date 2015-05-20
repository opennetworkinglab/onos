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
 * @param <S> subject class
 */
@Beta
public abstract class ConfigFactory<S> {

    private final Class<S> subjectClass;
    private final String key;

    /**
     * Creates a new configuration factory for the specified class of subjects
     * and bound to the given subject configuration key.
     *
     * @param subjectClass subject class
     * @param key          subject configuration key
     */
    protected ConfigFactory(Class<S> subjectClass, String key) {
        this.subjectClass = subjectClass;
        this.key = key;
    }

    /**
     * Returns the class of the subject to which this factory applies.
     *
     * @return subject type
     */
    public Class<S> subjectClass() {
        return subjectClass;
    }

    /**
     * Returns the key to which produced configurations should be bound.
     *
     * @return subject configuration key
     */
    public String key() {
        return key;
    }

    /**
     * Creates a new but uninitialized configuration. Framework will initialize
     * the configuration via {@link Config#init} method.
     *
     * @return new uninitialized configuration
     */
    public abstract Config<S> createConfig();

}
