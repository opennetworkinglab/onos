/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.resource;

import com.google.common.base.Objects;
import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of global unique ID for ResourceConsumer object.
 */
public class ResourceConsumerId {
    private final String className;
    private final long value;

    // Constructor for serializer.
    protected ResourceConsumerId() {
        this.className = null;
        this.value = 0L;
    }

    /**
     * Constructor with specifying every fields.
     *
     * @param value ID value unique within the given class
     * @param cls class of ResourceConsumer implementation
     */
    ResourceConsumerId(long value, Class<?> cls) {
        this.className = checkNotNull(cls.getName());
        this.value = value;
    }

    /**
     * Checks if the consumer is an instance of given class.
     *
     * @param cls class object
     * @return result of check
     */
    public boolean isClassOf(Class<?> cls) {
        return checkNotNull(cls).getName().equals(className);
    }

    /**
     * Returns class name of the consumer.
     *
     * @return class name of the consumer in String
     */
    public String consumerClass() {
        return className;
    }

    /**
     * Returns ID value.
     *
     * @return ID value
     */
    public long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceConsumerId that = (ResourceConsumerId) o;
        return Objects.equal(className, that.className) &&
                Objects.equal(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(className, value);
    }

    /**
     * Creates ResourceConsumerId from given value and class.
     *
     * @param <T> resource consumer class type
     * @param value ID value unique within the given class
     * @param cls class of ResourceConsumer implementation
     * @return created ResourceConsumerId object
     */
    public static <T extends ResourceConsumer> ResourceConsumerId of(long value, Class<T> cls) {
        return new ResourceConsumerId(value, cls);
    }

    /**
     * Creates ResourceConsumerId instance from Identifier object.
     *
     * @param <T> resource consumer class type
     * @param id identifier object backed by Long value
     * @return created ResourceConsumerId object
     */
    public static <T extends Identifier<Long> & ResourceConsumer> ResourceConsumerId of(T id) {
        return new ResourceConsumerId(id.id(), id.getClass());
    }
}
