/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 */

package org.onosproject.drivers.bmv2.api.runtime;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The class that represents a multicast node in BMv2 PRE.
 */
public class Bmv2PreNode {
    //replication id
    private final Integer rid;
    private final String portMap;
    private Integer l1Handle;

    public Bmv2PreNode(Integer rid, String portMap) {
        this.rid = checkNotNull(rid, "rid argument can not be null");
        this.portMap = checkNotNull(portMap, "portMap argument can not be null");
    }

    public static Bmv2PreNodeBuilder builder() {
        return new Bmv2PreNodeBuilder();
    }

    public Integer rid() {
        return rid;
    }

    public String portMap() {
        return portMap;
    }

    public Integer l1Handle() {
        return l1Handle;
    }

    public void setL1Handle(Integer l1Handle) {
        this.l1Handle = l1Handle;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rid, portMap, l1Handle);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2PreNode other = (Bmv2PreNode) obj;
        return Objects.equal(this.rid, other.rid)
                && Objects.equal(this.portMap, other.portMap);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("rid", rid)
                .add("portMap", portMap)
                .add("l1Handle", l1Handle)
                .toString();
    }

    public static final class Bmv2PreNodeBuilder {
        //replication id
        private Integer rid;
        private String portMap;
        private Integer l1Handle;

        private Bmv2PreNodeBuilder() {
        }

        public Bmv2PreNodeBuilder withRid(Integer rid) {
            this.rid = rid;
            return this;
        }

        public Bmv2PreNodeBuilder withPortMap(String portMap) {
            this.portMap = portMap;
            return this;
        }

        public Bmv2PreNodeBuilder withL1Handle(Integer l1Handle) {
            this.l1Handle = l1Handle;
            return this;
        }

        public Bmv2PreNode build() {
            Bmv2PreNode bmv2PreNode = new Bmv2PreNode(rid, portMap);
            bmv2PreNode.setL1Handle(l1Handle);
            return bmv2PreNode;
        }
    }
}
