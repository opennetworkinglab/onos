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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.Beta;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base abstraction of a configuration facade for a specific subject. Derived
 * classes should keep all state in the specified JSON tree.
 *
 * @param <S> type of subject
 */
@Beta
public abstract class Config<S> {

    protected ObjectMapper mapper;
    protected ObjectNode node;
    private S subject;
    private ConfigApplyDelegate<S> delegate;

    /**
     * Returns the specific subject to which this configuration pertains.
     *
     * @return configuration subject
     */
    S subject() {
        return subject;
    }

    /**
     * Initializes the configuration behaviour with necessary context.
     *
     * @param subject  configuration subject
     * @param node     JSON object node where configuration data is stored
     * @param mapper   JSON object mapper
     * @param delegate delegate context
     */
    public void init(S subject, ObjectNode node, ObjectMapper mapper,
                     ConfigApplyDelegate<S> delegate) {
        this.subject = checkNotNull(subject);
        this.node = checkNotNull(node);
        this.mapper = checkNotNull(mapper);
        this.delegate = checkNotNull(delegate);
    }

    /**
     * Applies any configuration changes made via this configuration.
     */
    public void apply() {
        delegate.onApply(this);
    }

}
