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
package org.onosproject.ofagent.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onosproject.ofagent.api.OFSwitchCapabilities;
import org.projectfloodlight.openflow.protocol.OFCapabilities;

import java.util.Set;

/**
 * Implementation of openflow switch capabilities.
 */
public final class DefaultOFSwitchCapabilities implements OFSwitchCapabilities {

    private final Set<OFCapabilities> ofCapabilities;

    private DefaultOFSwitchCapabilities(Set<OFCapabilities> ofSwitchCapabilities) {
        this.ofCapabilities = ImmutableSet.copyOf(ofSwitchCapabilities);
    }

    @Override
    public Set<OFCapabilities> ofSwitchCapabilities() {
        return ofCapabilities;
    }

    /**
     * Returns DefaultOFSwitchCapabilities builder object.
     *
     * @return DefaultOFSwitchCapabilities builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements OFSwitchCapabilities.Builder {
        private Set<OFCapabilities> ofCapabilities;

        private Builder() {
            ofCapabilities = Sets.newHashSet();
        }

        @Override
        public Builder flowStats() {
            ofCapabilities.add(OFCapabilities.FLOW_STATS);
            return this;
        }

        @Override
        public Builder tableStats() {
            ofCapabilities.add(OFCapabilities.TABLE_STATS);
            return this;
        }

        @Override
        public Builder portStats() {
            ofCapabilities.add(OFCapabilities.PORT_STATS);
            return this;
        }

        @Override
        public Builder groupStats() {
            ofCapabilities.add(OFCapabilities.GROUP_STATS);
            return this;
        }

        @Override
        public Builder ipReasm() {
            ofCapabilities.add(OFCapabilities.IP_REASM);
            return this;
        }

        @Override
        public Builder queueStats() {
            ofCapabilities.add(OFCapabilities.QUEUE_STATS);
            return this;
        }

        @Override
        public Builder portBlocked() {
            ofCapabilities.add(OFCapabilities.PORT_BLOCKED);
            return this;
        }

        @Override
        public OFSwitchCapabilities build() {
            return new DefaultOFSwitchCapabilities(ofCapabilities);
        }
    }
}
