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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.Acs;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.instance.Nes;

/**
 * Represents the builder implementation of instance.
 */
public class InstanceBuilder implements Instance.InstanceBuilder {

    private String id;
    private String name;
    private String mode;
    private Nes nes;
    private Acs acs;

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String mode() {
        return mode;
    }

    @Override
    public Nes nes() {
        return nes;
    }

    @Override
    public Acs acs() {
        return acs;
    }

    @Override
    public InstanceBuilder id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public InstanceBuilder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public InstanceBuilder mode(String mode) {
        this.mode = mode;
        return this;
    }

    @Override
    public InstanceBuilder nes(Nes nes) {
        this.nes = nes;
        return this;
    }

    @Override
    public InstanceBuilder acs(Acs acs) {
        this.acs = acs;
        return this;
    }

    @Override
    public Instance build() {
        return new InstanceImpl(this);
    }

    /**
     * Creates an instance of instanceBuilder.
     */
    public InstanceBuilder() {
    }


    /**
     * Represents the implementation of instance.
     */
    public final class InstanceImpl implements Instance {

        private String id;
        private String name;
        private String mode;
        private Nes nes;
        private Acs acs;

        @Override
        public String id() {
            return id;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String mode() {
            return mode;
        }

        @Override
        public Nes nes() {
            return nes;
        }

        @Override
        public Acs acs() {
            return acs;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, mode, nes, acs);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof InstanceImpl) {
                InstanceImpl other = (InstanceImpl) obj;
                return
                     Objects.equals(id, other.id) &&
                     Objects.equals(name, other.name) &&
                     Objects.equals(mode, other.mode) &&
                     Objects.equals(nes, other.nes) &&
                     Objects.equals(acs, other.acs);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("id", id)
                .add("name", name)
                .add("mode", mode)
                .add("nes", nes)
                .add("acs", acs)
                .toString();
        }

        /**
         * Creates an instance of instanceImpl.
         *
         * @param builderObject builder object of instance
         */
        public InstanceImpl(InstanceBuilder builderObject) {
            this.id = builderObject.id();
            this.name = builderObject.name();
            this.mode = builderObject.mode();
            this.nes = builderObject.nes();
            this.acs = builderObject.acs();
        }
    }
}