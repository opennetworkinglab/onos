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
package org.onosproject.store.primitives.impl;

import java.util.function.BiFunction;

import org.onosproject.core.Version;
import org.onosproject.store.service.AtomicValueBuilder;
import org.onosproject.store.service.RevisionType;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Topic;
import org.onosproject.store.service.TopicBuilder;

/**
 * Default topic builder.
 */
public class DefaultDistributedTopicBuilder<T> extends TopicBuilder<T> {
    private final AtomicValueBuilder<T> valueBuilder;

    public DefaultDistributedTopicBuilder(AtomicValueBuilder<T> valueBuilder) {
        this.valueBuilder = valueBuilder;
    }

    @Override
    public TopicBuilder<T> withName(String name) {
        valueBuilder.withName(name);
        return this;
    }

    @Override
    public TopicBuilder<T> withSerializer(Serializer serializer) {
        valueBuilder.withSerializer(serializer);
        return this;
    }

    @Override
    public TopicBuilder<T> withVersion(Version version) {
        valueBuilder.withVersion(version);
        return this;
    }

    @Override
    public TopicBuilder<T> withRevisionType(RevisionType revisionType) {
        valueBuilder.withRevisionType(revisionType);
        return this;
    }

    @Override
    public TopicBuilder<T> withCompatibilityFunction(BiFunction<T, Version, T> compatibilityFunction) {
        valueBuilder.withCompatibilityFunction(compatibilityFunction);
        return this;
    }

    @Override
    public Topic<T> build() {
        return new DefaultDistributedTopic<>(valueBuilder.build());
    }
}
