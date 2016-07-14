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
 * DSCP value extension implementation.
 */
public class DefaultExtDscpValue implements ExtDscpValue {

    private List<ExtOperatorValue> dscpValue;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtDscpValue which contains dscp operator value list.
     *
     * @param dscpValue is a dscp value rule list
     * @param type ExtType type
     */
    DefaultExtDscpValue(List<ExtOperatorValue> dscpValue, ExtType type) {
        this.dscpValue = dscpValue;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public List<ExtOperatorValue> dscpValue() {
        return dscpValue;
    }

    @Override
    public boolean exactMatch(ExtDscpValue value) {
        return this.equals(value) &&
                Objects.equals(this.dscpValue, value.dscpValue())
                && Objects.equals(this.type, value.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(dscpValue, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtDscpValue) {
            DefaultExtDscpValue that = (DefaultExtDscpValue) obj;
            return Objects.equals(dscpValue, that.dscpValue())
                    && Objects.equals(this.type, that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("dscpValue", dscpValue.toString())
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for extension dscp value.
     */
    public static class Builder implements ExtDscpValue.Builder {
        private List<ExtOperatorValue> dscpValue;
        private ExtType type;

        @Override
        public Builder setDscpValue(List<ExtOperatorValue> dscpValue) {
            this.dscpValue = dscpValue;
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtDscpValue build() {
            checkNotNull(dscpValue, "dscpValue cannot be null");
            return new DefaultExtDscpValue(dscpValue, type);
        }
    }
}
