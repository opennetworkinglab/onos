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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs.ac;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.onosproject.yang.gen.v1.net.l3vpn.type.rev20160701.netl3vpntype.l2access.Port;

/**
 * Represents the builder implementation of l2Access.
 */
public class L2AccessBuilder implements L2Access.L2AccessBuilder {

    private String accessType;
    private Port port;

    @Override
    public String accessType() {
        return accessType;
    }

    @Override
    public Port port() {
        return port;
    }

    @Override
    public L2AccessBuilder accessType(String accessType) {
        this.accessType = accessType;
        return this;
    }

    @Override
    public L2AccessBuilder port(Port port) {
        this.port = port;
        return this;
    }

    @Override
    public L2Access build() {
        return new L2AccessImpl(this);
    }

    /**
     * Creates an instance of l2AccessBuilder.
     */
    public L2AccessBuilder() {
    }


    /**
     * Represents the implementation of l2Access.
     */
    public final class L2AccessImpl implements L2Access {

        private String accessType;
        private Port port;

        @Override
        public String accessType() {
            return accessType;
        }

        @Override
        public Port port() {
            return port;
        }

        @Override
        public int hashCode() {
            return Objects.hash(accessType, port);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof L2AccessImpl) {
                L2AccessImpl other = (L2AccessImpl) obj;
                return
                     Objects.equals(accessType, other.accessType) &&
                     Objects.equals(port, other.port);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("accessType", accessType)
                .add("port", port)
                .toString();
        }

        /**
         * Creates an instance of l2AccessImpl.
         *
         * @param builderObject builder object of l2Access
         */
        public L2AccessImpl(L2AccessBuilder builderObject) {
            this.accessType = builderObject.accessType();
            this.port = builderObject.port();
        }
    }
}