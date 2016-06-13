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
 * Icmp code extension implementation.
 */
public class DefaultExtIcmpCode implements ExtIcmpCode {

    private List<ExtOperatorValue> icmpCode;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtIcmpCode which contains ICMP code operator value list.
     *
     * @param icmpCode is a icmp code rule list
     * @param type ExtType type
     */
    DefaultExtIcmpCode(List<ExtOperatorValue> icmpCode, ExtType type) {
        this.icmpCode = icmpCode;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public List<ExtOperatorValue> icmpCode() {
        return icmpCode;
    }

    @Override
    public boolean exactMatch(ExtIcmpCode icmpCode) {
        return this.equals(icmpCode) &&
                Objects.equals(this.icmpCode, icmpCode.icmpCode())
                && Objects.equals(this.type, icmpCode.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(icmpCode, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtIcmpCode) {
            DefaultExtIcmpCode that = (DefaultExtIcmpCode) obj;
            return Objects.equals(icmpCode, that.icmpCode())
                    && Objects.equals(this.type, that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("icmpCode", icmpCode.toString())
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for extension icmp type.
     */
    public static class Builder implements ExtIcmpCode.Builder {
        private List<ExtOperatorValue> icmpCode;
        private ExtType type;

        @Override
        public Builder setIcmpCode(List<ExtOperatorValue> icmpCode) {
            this.icmpCode = icmpCode;
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtIcmpCode build() {
            checkNotNull(icmpCode, "icmpCode cannot be null");
            return new DefaultExtIcmpCode(icmpCode, type);
        }
    }
}
