/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.intent;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.NetworkResource;

import java.util.Collection;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Abstraction of an application level intent.
 * <p>
 * Make sure that an Intent should be immutable when a new type is defined.
 * </p>
 */
public abstract class Intent {

    private final IntentId id;

    private final ApplicationId appId;
    private final String key; // TODO make this a class

    private final Collection<NetworkResource> resources;

    private static IdGenerator idGenerator;

    /**
     * Constructor for serializer.
     */
    protected Intent() {
        this.id = null;
        this.appId = null;
        this.key = null;
        this.resources = null;
    }

    /**
     * Creates a new intent.
     *
     * @param appId         application identifier
     * @param resources     required network resources (optional)
     */
    protected Intent(ApplicationId appId,
                     Collection<NetworkResource> resources) {
        this(appId, null, resources);
    }

        /**
         * Creates a new intent.
         *
         * @param appId         application identifier
         * @param key           optional key
         * @param resources     required network resources (optional)
         */
    protected Intent(ApplicationId appId,
                     String key,
                     Collection<NetworkResource> resources) {
        checkState(idGenerator != null, "Id generator is not bound.");
        this.id = IntentId.valueOf(idGenerator.getNewId());
        this.appId = checkNotNull(appId, "Application ID cannot be null");
        this.key = (key != null) ? key : id.toString(); //FIXME
        this.resources = checkNotNull(resources);
    }

    /**
     * Returns the intent identifier.
     *
     * @return intent fingerprint
     */
    public IntentId id() {
        return id;
    }

    /**
     * Returns the identifier of the application that requested the intent.
     *
     * @return application identifier
     */
    public ApplicationId appId() {
        return appId;
    }

    /**
     * Returns the collection of resources required for this intent.
     *
     * @return collection of resources; may be null
     */
    public Collection<NetworkResource> resources() {
        return resources;
    }

    /**
     * Indicates whether or not the intent is installable.
     *
     * @return true if installable
     */
    public boolean isInstallable() {
        return false;
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Intent other = (Intent) obj;
        return this.id().equals(other.id());
    }

    /**
     * Binds an id generator for unique intent id generation.
     *
     * Note: A generator cannot be bound if there is already a generator bound.
     * @param newIdGenerator id generator
     */
    public static void bindIdGenerator(IdGenerator newIdGenerator) {
        checkState(idGenerator == null, "Id generator is already bound.");
        idGenerator = checkNotNull(newIdGenerator);
    }

    /**
     * Unbinds an id generator.
     *
     * Note: The caller must provide the old id generator to succeed.
     * @param oldIdGenerator the current id generator
     */
    public static void unbindIdGenerator(IdGenerator oldIdGenerator) {
        if (Objects.equals(idGenerator, oldIdGenerator)) {
            idGenerator = null;
        }
    }

    public String key() {
        return key;
    }
}
