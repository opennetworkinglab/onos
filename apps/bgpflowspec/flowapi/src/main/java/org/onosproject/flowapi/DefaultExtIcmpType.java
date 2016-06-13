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
 * Icmp type extension implementation.
 */
public class DefaultExtIcmpType implements ExtIcmpType {

    private List<ExtOperatorValue> icmpType;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtIcmpType which contains ICMP type operator value list.
     *
     * @param icmpType is a icmp type rule list
     * @param type ExtType type
     */
    DefaultExtIcmpType(List<ExtOperatorValue> icmpType, ExtType type) {
        this.icmpType = icmpType;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public List<ExtOperatorValue> icmpType() {
        return icmpType;
    }

    @Override
    public boolean exactMatch(ExtIcmpType icmpType) {
        return this.equals(icmpType) &&
                Objects.equals(this.icmpType, icmpType.icmpType())
                && Objects.equals(this.type, icmpType.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(icmpType, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtIcmpType) {
            DefaultExtIcmpType that = (DefaultExtIcmpType) obj;
            return Objects.equals(icmpType, that.icmpType())
                    && Objects.equals(this.type, that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("icmpType", icmpType.toString())
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for extension icmp type.
     */
    public static class Builder implements ExtIcmpType.Builder {
        private List<ExtOperatorValue> icmpType;
        private ExtType type;

        @Override
        public Builder setIcmpType(List<ExtOperatorValue> icmpType) {
            this.icmpType = icmpType;
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtIcmpType build() {
            checkNotNull(icmpType, "icmpType cannot be null");
            return new DefaultExtIcmpType(icmpType, type);
        }
    }
}
