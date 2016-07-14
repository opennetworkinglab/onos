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
 * Key Name for the extension implementation.
 */
public class DefaultExtKeyName implements ExtKeyName {

    private String keyName;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtKeyName which name for the flow rule.
     *
     * @param keyName is a key for the extended rule
     * @param type ExtType type
     */
    DefaultExtKeyName(String keyName, ExtType type) {
        this.keyName = keyName;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public String keyName() {
        return keyName;
    }

    @Override
    public boolean exactMatch(ExtKeyName value) {
        return this.equals(value) &&
                Objects.equals(this.keyName, value.keyName())
                && Objects.equals(this.type, value.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyName, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtKeyName) {
            DefaultExtKeyName that = (DefaultExtKeyName) obj;
            return Objects.equals(keyName, that.keyName())
                    && Objects.equals(this.type, that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("keyName", keyName.toString())
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for Extended key name for the rule.
     */
    public static class Builder implements ExtKeyName.Builder {
        private String keyName;
        private ExtType type;

        @Override
        public Builder setKeyName(String keyName) {
            this.keyName = keyName;
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtKeyName build() {
            checkNotNull(keyName, "keyName cannot be null");
            return new DefaultExtKeyName(keyName, type);
        }
    }
}
