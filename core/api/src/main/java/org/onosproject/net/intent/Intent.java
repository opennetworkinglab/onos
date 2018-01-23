/*
 * Copyright 2014-present Open Networking Foundation
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

import com.google.common.annotations.Beta;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.ResourceGroup;

import java.util.Collection;

import static com.google.common.base.Preconditions.*;

/**
 * Abstraction of an application level intent.
 * <p>
 * Make sure that an Intent should be immutable when a new type is defined.
 * </p>
 */
@Beta
public abstract class Intent {

    private final IntentId id;

    private final ApplicationId appId;
    private final Key key;

    private final int priority;
    public static final int DEFAULT_INTENT_PRIORITY = 100;
    public static final int MAX_PRIORITY = (1 << 16) - 1;
    public static final int MIN_PRIORITY = 1;

    private final Collection<NetworkResource> resources;
    private final ResourceGroup resourceGroup;

    private static IdGenerator idGenerator;
    private static final Object ID_GENERATOR_LOCK = new Object();

    /**
     * Constructor for serializer.
     */
    protected Intent() {
        this.id = null;
        this.appId = null;
        this.key = null;
        this.resources = null;
        this.priority = DEFAULT_INTENT_PRIORITY;
        this.resourceGroup = null;
    }

    /**
     * Creates a new intent.
     * @param appId     application identifier
     * @param key       optional key
     * @param resources required network resources (optional)
     * @param priority  flow rule priority
     * @param resourceGroup the resource group for intent
     */
    protected Intent(ApplicationId appId,
                     Key key,
                     Collection<NetworkResource> resources,
                     int priority,
                     ResourceGroup resourceGroup) {
        checkState(idGenerator != null, "Id generator is not bound.");
        checkArgument(priority <= MAX_PRIORITY && priority >= MIN_PRIORITY);
        this.id = IntentId.valueOf(idGenerator.getNewId());
        this.appId = checkNotNull(appId, "Application ID cannot be null");
        this.key = (key != null) ? key : Key.of(id.fingerprint(), appId);
        this.priority = priority;
        this.resources = checkNotNull(resources);
        this.resourceGroup = resourceGroup;
    }

    /**
     * Abstract builder for intents.
     */
    public abstract static class Builder {
        protected ApplicationId appId;
        protected Key key;
        protected int priority = Intent.DEFAULT_INTENT_PRIORITY;
        protected Collection<NetworkResource> resources;
        protected ResourceGroup resourceGroup;

        /**
         * Creates a new empty builder.
         */
        protected Builder() {
        }

        /**
         * Creates a new builder pre-populated with the information in the given
         * intent.
         *
         * @param intent initial intent
         */
        protected Builder(Intent intent) {
            this.appId(intent.appId())
                    .key(intent.key())
                    .priority(intent.priority())
                    .resourceGroup(intent.resourceGroup());
        }

        /**
         * Sets the application id for the intent that will be built.
         *
         * @param appId application id to use for built intent
         * @return this builder
         */
        public Builder appId(ApplicationId appId) {
            this.appId = appId;
            return this;
        }

        /**
         * Sets the key for the intent that will be built.
         *
         * @param key key to use for built intent
         * @return this builder
         */
        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the priority for the intent that will be built.
         *
         * @param priority priority to use for built intent
         * @return this builder
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets the collection of resources required for this intent.
         *
         * @param resources collection of resources
         * @return this builder
         */
        public Builder resources(Collection<NetworkResource> resources) {
            this.resources = resources;
            return this;
        }

        /**
         * Sets the resource group for this intent.
         *
         * @param resourceGroup the resource group
         * @return this builder
         */
        public Builder resourceGroup(ResourceGroup resourceGroup) {
            this.resourceGroup = resourceGroup;
            return this;
        }
    }

    /**
     * Returns the intent object identifier.
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
     * Returns the priority of the intent.
     *
     * @return intent priority
     */
    public int priority() {
        return priority;
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
     * Returns the resource group for this intent.
     *
     * @return the resource group; may be null
     */
    public ResourceGroup resourceGroup() {
        return resourceGroup;
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
     *
     * @param newIdGenerator id generator
     */
    public static void bindIdGenerator(IdGenerator newIdGenerator) {
        synchronized (ID_GENERATOR_LOCK) {
            checkState(idGenerator == null, "Id generator is already bound.");
            idGenerator = checkNotNull(newIdGenerator);
        }
    }

    /**
     * Unbinds an id generator.
     *
     * Note: The caller must provide the old id generator to succeed.
     *
     * @param oldIdGenerator the current id generator
     */
    public static void unbindIdGenerator(IdGenerator oldIdGenerator) {
        synchronized (ID_GENERATOR_LOCK) {
            if (idGenerator == oldIdGenerator) {
                idGenerator = null;
            }
        }
    }

    /**
     * Returns the key to identify an "Intent".
     * <p>
     * When an Intent is updated,
     * (e.g., flow is re-routed in reaction to network topology change)
     * related Intent object's {@link IntentId} may change,
     * but the key will remain unchanged.
     *
     * @return key
     */
    public Key key() {
        return key;
    }
}
