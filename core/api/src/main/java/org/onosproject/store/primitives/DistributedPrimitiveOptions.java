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
package org.onosproject.store.primitives;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.Version;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.RevisionType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract builder for distributed primitives.
 *
 * @param <O> distributed primitive options type
 */
public abstract class DistributedPrimitiveOptions<O extends DistributedPrimitiveOptions<O>> {

    private final DistributedPrimitive.Type type;
    private String name;
    private ApplicationId applicationId;
    private Serializer serializer;
    private boolean partitionsDisabled = false;
    private boolean meteringDisabled = false;
    private boolean readOnly = false;
    private boolean relaxedReadConsistency = false;
    private Version version;
    private RevisionType revisionType;

    public DistributedPrimitiveOptions(DistributedPrimitive.Type type) {
        this.type = type;
    }

    /**
     * Sets the primitive name.
     *
     * @param name primitive name
     * @return this builder
     */
    public O withName(String name) {
        this.name = name;
        return (O) this;
    }

    /**
     * Sets the serializer to use for transcoding info held in the primitive.
     *
     * @param serializer serializer
     * @return this builder
     */
    public O withSerializer(Serializer serializer) {
        this.serializer = serializer;
        return (O) this;
    }

    /**
     * Sets the application id that owns this primitive.
     *
     * @param applicationId application identifier
     * @return this builder
     */
    public O withApplicationId(ApplicationId applicationId) {
        this.applicationId = applicationId;
        return (O) this;
    }

    /**
     * Sets the primitive version.
     *
     * @param version the primitive version
     * @return this builder
     */
    public O withVersion(Version version) {
        this.version = version;
        return (O) this;
    }

    /**
     * Sets the primitive revision type.
     *
     * @param revisionType the revision type
     * @return this builder
     */
    public O withRevisionType(RevisionType revisionType) {
        this.revisionType = checkNotNull(revisionType);
        return (O) this;
    }

    /**
     * Disables state changing operations on the returned distributed primitive.
     * @return this builder
     */
    public O withUpdatesDisabled() {
        this.readOnly = true;
        return (O) this;
    }

    /**
     * Turns on relaxed consistency for read operations.
     * @return this builder
     */
    public O withRelaxedReadConsistency() {
        this.relaxedReadConsistency = true;
        return (O) this;
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
     * Returns the primitive version.
     *
     * @return the primitive version
     */
    public final Version version() {
        return version;
    }

    /**
     * Returns the primitive revision type.
     *
     * @return the primitive revision type
     */
    public RevisionType revisionType() {
        return revisionType;
    }
}
