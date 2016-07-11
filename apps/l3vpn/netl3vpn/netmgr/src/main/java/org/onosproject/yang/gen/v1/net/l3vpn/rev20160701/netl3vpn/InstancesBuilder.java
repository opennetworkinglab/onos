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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.Instance;

/**
 * Represents the builder implementation of instances.
 */
public class InstancesBuilder implements Instances.InstancesBuilder {

    private List<Instance> instance;

    @Override
    public List<Instance> instance() {
        return instance;
    }

    @Override
    public InstancesBuilder instance(List<Instance> instance) {
        this.instance = instance;
        return this;
    }

    @Override
    public Instances build() {
        return new InstancesImpl(this);
    }

    /**
     * Creates an instance of instancesBuilder.
     */
    public InstancesBuilder() {
    }


    /**
     * Represents the implementation of instances.
     */
    public final class InstancesImpl implements Instances {

        private List<Instance> instance;

        @Override
        public List<Instance> instance() {
            return instance;
        }

        @Override
        public int hashCode() {
            return Objects.hash(instance);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof InstancesImpl) {
                InstancesImpl other = (InstancesImpl) obj;
                return
                     Objects.equals(instance, other.instance);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("instance", instance)
                .toString();
        }

        /**
         * Creates an instance of instancesImpl.
         *
         * @param builderObject builder object of instances
         */
        public InstancesImpl(InstancesBuilder builderObject) {
            this.instance = builderObject.instance();
        }
    }
}