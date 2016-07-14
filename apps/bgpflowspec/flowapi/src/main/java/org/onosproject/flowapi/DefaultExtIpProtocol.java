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
package org.onosproject.flowapi;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Ip protocol extension implementation.
 */
public final class DefaultExtIpProtocol implements ExtIpProtocol {

    private List<ExtOperatorValue> ipProtocol;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtIpProtocol which contains Ip protocol list.
     *
     * @param ipProtocol Ip protocol operator value list
     * @param type BgpType type
     */
    DefaultExtIpProtocol(List<ExtOperatorValue> ipProtocol, ExtType type) {
        this.ipProtocol = ipProtocol;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public List<ExtOperatorValue> ipProtocol() {
        return ipProtocol;
    }

    @Override
    public boolean exactMatch(ExtIpProtocol ipProto) {
        return this.equals(ipProto) &&
                Objects.equals(this.ipProtocol, ipProto.ipProtocol())
                && Objects.equals(this.type, ipProto.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipProtocol, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtIpProtocol) {
            DefaultExtIpProtocol that = (DefaultExtIpProtocol) obj;
            return Objects.equals(ipProtocol, that.ipProtocol())
                    && Objects.equals(this.type, that.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("ipProtocol", ipProtocol.toString())
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for extension Ip protocol.
     */
    public static class Builder implements ExtIpProtocol.Builder {
        private List<ExtOperatorValue> ipProtocol;
        private ExtType type;

        @Override
        public Builder setIpProtocol(List<ExtOperatorValue> ipProtocol) {
            this.ipProtocol = ipProtocol;
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtIpProtocol build() {
            checkNotNull(ipProtocol, "Ip protocol cannot be null");
            return new DefaultExtIpProtocol(ipProtocol, type);
        }
    }
}

