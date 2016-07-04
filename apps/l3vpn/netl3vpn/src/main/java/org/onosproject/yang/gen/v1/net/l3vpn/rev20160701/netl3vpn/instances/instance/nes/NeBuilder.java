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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.instance.nes;

import com.google.common.base.MoreObjects;
import java.util.Objects;

/**
 * Represents the builder implementation of ne.
 */
public class NeBuilder implements Ne.NeBuilder {

    private String id;

    @Override
    public String id() {
        return id;
    }

    @Override
    public NeBuilder id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public Ne build() {
        return new NeImpl(this);
    }

    /**
     * Creates an instance of neBuilder.
     */
    public NeBuilder() {
    }


    /**
     * Represents the implementation of ne.
     */
    public final class NeImpl implements Ne {

        private String id;

        @Override
        public String id() {
            return id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof NeImpl) {
                NeImpl other = (NeImpl) obj;
                return
                     Objects.equals(id, other.id);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("id", id)
                .toString();
        }

        /**
         * Creates an instance of neImpl.
         *
         * @param builderObject builder object of ne
         */
        public NeImpl(NeBuilder builderObject) {
            this.id = builderObject.id();
        }
    }
}