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

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Extended traffic marking implementation.
 */
public class DefaultExtTrafficMarking implements ExtTrafficMarking {

    private byte marking;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtTrafficMarking which contains traffic marking byte.
     *
     * @param marking is a marking rule
     * @param type ExtType type
     */
    DefaultExtTrafficMarking(byte marking, ExtType type) {
        this.marking = marking;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public byte marking() {
        return marking;
    }

    @Override
    public boolean exactMatch(ExtTrafficMarking value) {
        return this.equals(value) &&
                Objects.equals(this.marking, value.marking())
                && Objects.equals(this.type, value.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(marking, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtTrafficMarking) {
            DefaultExtTrafficMarking that = (DefaultExtTrafficMarking) obj;
            return Objects.equals(marking, that.marking())
                    && Objects.equals(this.type, that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("marking", marking)
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for extended traffic marking value rule.
     */
    public static class Builder implements ExtTrafficMarking.Builder {
        private byte marking;
        private ExtType type;

        @Override
        public Builder setMarking(byte marking) {
            this.marking = marking;
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtTrafficMarking build() {
            checkNotNull(marking, "marking cannot be null");
            return new DefaultExtTrafficMarking(marking, type);
        }
    }
}
