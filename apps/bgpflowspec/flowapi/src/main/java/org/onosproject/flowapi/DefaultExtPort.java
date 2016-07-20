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
 * Port extension implementation.
 */
public class DefaultExtPort implements ExtPort {

    private List<ExtOperatorValue> port;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtPort which contains port list.
     *
     * @param port is a port rule list
     * @param type ExtType type
     */
    DefaultExtPort(List<ExtOperatorValue> port, ExtType type) {
        this.port = port;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public List<ExtOperatorValue> port() {
        return port;
    }

    @Override
    public boolean exactMatch(ExtPort port) {
        return this.equals(port) &&
                Objects.equals(this.port, port.port())
                && Objects.equals(this.type, port.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtPort) {
            DefaultExtPort that = (DefaultExtPort) obj;
            return Objects.equals(port, that.port())
                    && Objects.equals(this.type, that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("port", port.toString())
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for extension port.
     */
    public static class Builder implements ExtPort.Builder {
        private List<ExtOperatorValue> port;
        private ExtType type;

        @Override
        public Builder setPort(List<ExtOperatorValue> port) {
            this.port = port;
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtPort build() {
            checkNotNull(port, "port cannot be null");
            return new DefaultExtPort(port, type);
        }
    }
}
