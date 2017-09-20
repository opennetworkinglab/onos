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
package org.onosproject.net.behaviour;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.SparseAnnotations;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation of immutable patch interface description entity.
 */
public final class DefaultPatchDescription extends AbstractDescription
        implements PatchDescription {

    private final Optional<String> deviceId;
    private final String ifaceName;
    private final String peerName;

    private DefaultPatchDescription(Optional<String> deviceId,
                                    String ifaceName,
                                    String peerName,
                                    SparseAnnotations... annotations) {
        super(annotations);
        this.deviceId = deviceId;
        this.ifaceName = ifaceName;
        this.peerName = peerName;
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
    public String peer() {
        return peerName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, ifaceName, peerName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultPatchDescription) {
            final DefaultPatchDescription that = (DefaultPatchDescription) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.deviceId, that.deviceId) &&
                    Objects.equals(this.ifaceName, that.ifaceName) &&
                    Objects.equals(this.peerName, that.peerName);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId)
                .add("ifaceName", ifaceName)
                .add("peerName", peerName)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return default patch description builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements PatchDescription.Builder {

        private Optional<String> deviceId = Optional.empty();
        private String ifaceName;
        private String peerName;

        private Builder() {
        }

        @Override
        public PatchDescription build() {
            return new DefaultPatchDescription(deviceId, ifaceName, peerName);
        }

        @Override
        public PatchDescription.Builder deviceId(String deviceId) {
            this.deviceId = Optional.ofNullable(deviceId);
            return this;
        }

        @Override
        public PatchDescription.Builder ifaceName(String ifaceName) {
            checkArgument(!Strings.isNullOrEmpty(ifaceName));
            this.ifaceName = ifaceName;
            return this;
        }

        @Override
        public PatchDescription.Builder peer(String peerName) {
            checkArgument(!Strings.isNullOrEmpty(peerName));
            this.peerName = peerName;
            return this;
        }
    }
}
