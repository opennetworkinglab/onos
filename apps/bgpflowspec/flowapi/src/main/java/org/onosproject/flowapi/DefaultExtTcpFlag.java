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
 * Tcp flag extension implementation.
 */
public class DefaultExtTcpFlag implements ExtTcpFlag {

    private List<ExtOperatorValue> tcpFlag;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtTcpFlag which contains tcp flag.
     *
     * @param tcpFlag is a Tcp Flag rule list
     * @param type ExtType type
     */
    DefaultExtTcpFlag(List<ExtOperatorValue> tcpFlag, ExtType type) {
        this.tcpFlag = tcpFlag;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public List<ExtOperatorValue> tcpFlag() {
        return tcpFlag;
    }

    @Override
    public boolean exactMatch(ExtTcpFlag flag) {
        return this.equals(tcpFlag) &&
                Objects.equals(this.tcpFlag, flag.tcpFlag())
                && Objects.equals(this.type, flag.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(tcpFlag, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtTcpFlag) {
            DefaultExtTcpFlag that = (DefaultExtTcpFlag) obj;
            return Objects.equals(tcpFlag, that.tcpFlag())
                    && Objects.equals(this.type, that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("tcpFlag", tcpFlag.toString())
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for extension tcp flag.
     */
    public static class Builder implements ExtTcpFlag.Builder {
        private List<ExtOperatorValue> tcpFlag;
        private ExtType type;

        @Override
        public Builder setTcpFlag(List<ExtOperatorValue> tcpFlag) {
            this.tcpFlag = tcpFlag;
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtTcpFlag build() {
            checkNotNull(tcpFlag, "tcpFlag cannot be null");
            return new DefaultExtTcpFlag(tcpFlag, type);
        }
    }
}
