/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.behaviour;

import com.google.common.base.Strings;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.SparseAnnotations;

import com.google.common.base.MoreObjects;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of immutable tunnel interface description entity.
 */
public final class DefaultTunnelDescription extends AbstractDescription
        implements TunnelDescription {

    private final Optional<String> deviceId;
    private final String ifaceName;
    private final Type type;
    private final Optional<TunnelEndPoint> local;
    private final Optional<TunnelEndPoint> remote;
    private final Optional<TunnelKey> key;

    /**
     * Creates a tunnel description using the supplied information.
     *
     * @param ifaceName tunnel interface ifaceName
     * @param local source tunnel endpoint
     * @param remote destination tunnel endpoint
     * @param type tunnel type
     * @param annotations optional key/value annotations
     */
    private DefaultTunnelDescription(Optional<String> deviceId,
                                     String ifaceName,
                                     Type type,
                                     Optional<TunnelEndPoint> local,
                                     Optional<TunnelEndPoint> remote,
                                     Optional<TunnelKey> key,
                                     SparseAnnotations... annotations) {
        super(annotations);
        this.deviceId = deviceId;
        this.ifaceName = checkNotNull(ifaceName);
        this.type = type;
        this.local = local;
        this.remote = remote;
        this.key = key;
    }

    @Override
    public Optional<String> deviceId() {
        return deviceId;
    }

    @Override
    public String ifaceName() {
        return ifaceName;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public Optional<TunnelEndPoint> local() {
        return local;
    }

    @Override
    public Optional<TunnelEndPoint> remote() {
        return remote;
    }

    @Override
    public Optional<TunnelKey> key() {
        return key;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId)
                .add("ifaceName", ifaceName)
                .add("type", type)
                .add("local", local)
                .add("remote", remote)
                .add("key", key)
                .toString();
    }

    /**
     * Creates and returns a new builder instance.
     *
     * @return default tunnel description builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements TunnelDescription.Builder {
        private Optional<String> deviceId = Optional.empty();
        private String ifaceName;
        private Type type;
        private Optional<TunnelEndPoint> local = Optional.empty();
        private Optional<TunnelEndPoint> remote = Optional.empty();
        private Optional<TunnelKey> key = Optional.empty();
        private Optional<SparseAnnotations> otherConfigs = Optional.empty();

        private Builder() {
        }

        @Override
        public TunnelDescription build() {
            if (otherConfigs.isPresent()) {
                return new DefaultTunnelDescription(deviceId, ifaceName, type,
                                                    local, remote, key,
                                                    otherConfigs.get());
            } else {
                return new DefaultTunnelDescription(deviceId, ifaceName, type,
                                                    local, remote, key);
            }
        }

        @Override
        public Builder deviceId(String deviceId) {
            this.deviceId = Optional.ofNullable(deviceId);
            return this;
        }

        @Override
        public Builder ifaceName(String ifaceName) {
            checkArgument(!Strings.isNullOrEmpty(ifaceName));
            this.ifaceName = ifaceName;
            return this;
        }

        @Override
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder local(TunnelEndPoint endpoint) {
            local = Optional.ofNullable(endpoint);
            return this;
        }

        @Override
        public Builder remote(TunnelEndPoint endpoint) {
            remote = Optional.ofNullable(endpoint);
            return this;
        }

        @Override
        public Builder key(TunnelKey key) {
            this.key = Optional.ofNullable(key);
            return this;
        }

        @Override
        public Builder otherConfigs(SparseAnnotations configs) {
            otherConfigs = Optional.ofNullable(configs);
            return this;
        }
    }
}
