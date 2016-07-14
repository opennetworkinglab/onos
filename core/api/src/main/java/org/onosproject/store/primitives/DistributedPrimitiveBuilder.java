/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.store.primitives;

import org.onosproject.core.ApplicationId;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.Serializer;

/**
 * Abstract builder for distributed primitives.
 *
 * @param <T> distributed primitive type
 */
public abstract class DistributedPrimitiveBuilder<B extends DistributedPrimitiveBuilder<B, T>,
                                                  T extends DistributedPrimitive> {

    private DistributedPrimitive.Type type;
    private String name;
    private ApplicationId applicationId;
    private Serializer serializer;
    private boolean partitionsDisabled = false;
    private boolean meteringDisabled = false;
    private boolean readOnly = false;
    private boolean relaxedReadConsistency = false;

    public DistributedPrimitiveBuilder(DistributedPrimitive.Type type) {
        this.type = type;
    }

    /**
     * Sets the primitive name.
     *
     * @param name primitive name
     * @return this builder
     */
    public B withName(String name) {
        this.name = name;
        return (B) this;
    }

    /**
     * Sets the serializer to use for transcoding info held in the primitive.
     *
     * @param serializer serializer
     * @return this builder
     */
    public B withSerializer(Serializer serializer) {
        this.serializer = serializer;
        return (B) this;
    }

    /**
     * Sets the application id that owns this primitive.
     *
     * @param applicationId application identifier
     * @return this builder
     */
    public B withApplicationId(ApplicationId applicationId) {
        this.applicationId = applicationId;
        return (B) this;
    }

    /**
     * Creates this primitive on a special partition that comprises of all members in the cluster.
     * @deprecated usage of this method is discouraged for most common scenarios. Eventually it will be replaced
     * with a better alternative that does not exposes low level details. Until then avoid using this method.
     * @return this builder
     */
    @Deprecated
    public B withPartitionsDisabled() {
        this.partitionsDisabled = true;
        return (B) this;
    }

    /**
     * Disables recording usage stats for this primitive.
     * @deprecated usage of this method is discouraged for most common scenarios.
     * @return this builder
     */
    @Deprecated
    public B withMeteringDisabled() {
        this.meteringDisabled = true;
        return (B) this;
    }

    /**
     * Disables state changing operations on the returned distributed primitive.
     * @return this builder
     */
    public B withUpdatesDisabled() {
        this.readOnly = true;
        return (B) this;
    }

    /**
     * Turns on relaxed consistency for read operations.
     * @return this builder
     */
    public B withRelaxedReadConsistency() {
        this.relaxedReadConsistency = true;
        return (B) this;
    }

    /**
     * Returns if metering is enabled.
     *
     * @return {@code true} if yes; {@code false} otherwise
     */
    public final boolean meteringEnabled() {
        return !meteringDisabled;
    }

    /**
     * Returns if partitions are disabled.
     *
     * @return {@code true} if yes; {@code false} otherwise
     */
    public final boolean partitionsDisabled() {
        return partitionsDisabled;
    }

    /**
     * Returns if updates are disabled.
     *
     * @return {@code true} if yes; {@code false} otherwise
     */
    public final boolean readOnly() {
        return readOnly;
    }

    /**
     * Returns if consistency is relaxed for read operations.
     *
     * @return {@code true} if yes; {@code false} otherwise
     */
    public final boolean relaxedReadConsistency() {
        return relaxedReadConsistency;
    }

    /**
     * Returns the serializer.
     *
     * @return serializer
     */
    public final Serializer serializer() {
        return serializer;
    }

    /**
     * Returns the application identifier.
     *
     * @return application id
     */
    public final ApplicationId applicationId() {
        return applicationId;
    }

    /**
     * Returns the name of the primitive.
     *
     * @return primitive name
     */
    public final String name() {
        return name;
    }

    /**
     * Returns the primitive type.
     *
     * @return primitive type
     */
    public final DistributedPrimitive.Type type() {
        return type;
    }

    /**
     * Constructs an instance of the distributed primitive.
     * @return distributed primitive
     */
    public abstract T build();
}
