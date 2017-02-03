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
package org.onosproject.net.intent;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Collection;

import javax.annotation.concurrent.Immutable;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointDescription;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

/**
 * Installable Intent for the ProtectionEndpoint (head/tail).
 */
@Immutable
@Beta
public class ProtectionEndpointIntent extends Intent {

    private final DeviceId deviceId;
    private final ProtectedTransportEndpointDescription description;

    /**
     * Creates a ProtectionEndpointIntent by specific resource and description.
     *
     * @param appId application identification
     * @param key intent key
     * @param resources network resource to be set
     * @param priority priority to use for flows from this intent
     * @param deviceId target device id
     * @param description protected transport endpoint description of the intent
     * @deprecated 1.9.1
     */
    @Deprecated
    protected ProtectionEndpointIntent(ApplicationId appId, Key key,
                                       Collection<NetworkResource> resources,
                                       int priority,
                                       DeviceId deviceId,
                                       ProtectedTransportEndpointDescription description) {
        super(appId, key, resources, priority, null);

        this.deviceId = checkNotNull(deviceId);
        this.description = checkNotNull(description);
    }

    /**
     * Creates a ProtectionEndpointIntent by specific resource and description.
     *
     * @param appId application identification
     * @param key intent key
     * @param resources network resource to be set
     * @param priority priority to use for flows from this intent
     * @param deviceId target device id
     * @param description protected transport endpoint description of the intent
     * @param resourceGroup resource group for this intent
     */
    protected ProtectionEndpointIntent(ApplicationId appId, Key key,
                                       Collection<NetworkResource> resources,
                                       int priority,
                                       DeviceId deviceId,
                                       ProtectedTransportEndpointDescription description,
                                       ResourceGroup resourceGroup) {
        super(appId, key, resources, priority, resourceGroup);

        this.deviceId = checkNotNull(deviceId);
        this.description = checkNotNull(description);
    }

    /**
     * Returns the identifier of the device to be configured.
     *
     * @return the deviceId
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the description of this protection endpoint.
     *
     * @return the description
     */
    public ProtectedTransportEndpointDescription description() {
        return description;
    }

    @Override
    public boolean isInstallable() {
        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("key", key())
                .add("appId", appId())
                .add("priority", priority())
                .add("resources", resources())
                .add("deviceId", deviceId)
                .add("description", description)
                .toString();
    }

    /**
     * Returns a new {@link ProtectionEndpointIntent} builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ProtectionEndpointIntent}.
     */
    public static class Builder extends Intent.Builder {

        private DeviceId deviceId;
        private ProtectedTransportEndpointDescription description;

        /**
         * Creates a new empty builder.
         */
        protected Builder() {
            resources = ImmutableList.of();
        }

        /**
         * Creates a new builder pre-populated with the information in the given
         * intent.
         *
         * @param intent initial intent
         */
        protected Builder(ProtectionEndpointIntent intent) {
            super(intent);
        }

        // TODO remove these overrides
        @Override
        public Builder key(Key key) {
            super.key(key);
            return this;
        }

        @Override
        public Builder appId(ApplicationId appId) {
            super.appId(appId);
            return this;
        }

        @Override
        public Builder resources(Collection<NetworkResource> resources) {
            super.resources(resources);
            return this;
        }

        @Override
        public Builder priority(int priority) {
            super.priority(priority);
            return this;
        }

        @Override
        public Builder resourceGroup(ResourceGroup resourceGroup) {
            return (Builder) super.resourceGroup(resourceGroup);
        }

        public Builder deviceId(DeviceId deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder description(ProtectedTransportEndpointDescription description) {
            this.description = description;
            return this;
        }

        public ProtectionEndpointIntent build() {
            checkNotNull(key, "Key inherited from origin Intent expected.");
            return new ProtectionEndpointIntent(appId,
                                                key,
                                                resources,
                                                priority,
                                                deviceId,
                                                description,
                                                resourceGroup);
        }

    }



    /*
     * For serialization.
     */
    @SuppressWarnings("unused")
    private ProtectionEndpointIntent() {
        this.deviceId = null;
        this.description = null;
    }
}
