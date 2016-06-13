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

import org.onlab.packet.IpPrefix;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Ip prefix extension implementation.
 */
public final class DefaultExtPrefix implements ExtPrefix {

    private List<IpPrefix> prefix;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtPrefix which contains Ip prefix list.
     *
     * @param prefix Ip prefix list
     * @param type ExtType type
     */
    DefaultExtPrefix(List<IpPrefix> prefix, ExtType type) {
        this.prefix = prefix;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public List<IpPrefix> prefix() {
        return prefix;
    }

    @Override
    public boolean exactMatch(ExtPrefix prefix) {
        return this.equals(prefix) &&
                Objects.equals(this.prefix, prefix.prefix())
                && Objects.equals(this.type, prefix.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtPrefix) {
            DefaultExtPrefix that = (DefaultExtPrefix) obj;
            return Objects.equals(prefix, that.prefix())
                    && Objects.equals(this.type, that.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("prefix", prefix.toString())
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for extension Ip prefix.
     */
    public static class Builder implements ExtPrefix.Builder {
        private List<IpPrefix> prefix = new ArrayList<>();
        private ExtType type;

        @Override
        public Builder setPrefix(IpPrefix ip) {
            this.prefix.add(ip);
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtPrefix build() {
            checkNotNull(prefix, "Ip prefix cannot be null");
            return new DefaultExtPrefix(prefix, type);
        }
    }
}

