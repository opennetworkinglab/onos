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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.instance;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.instance.nes.Ne;

/**
 * Represents the builder implementation of nes.
 */
public class NesBuilder implements Nes.NesBuilder {

    private List<Ne> ne;

    @Override
    public List<Ne> ne() {
        return ne;
    }

    @Override
    public NesBuilder ne(List<Ne> ne) {
        this.ne = ne;
        return this;
    }

    @Override
    public Nes build() {
        return new NesImpl(this);
    }

    /**
     * Creates an instance of nesBuilder.
     */
    public NesBuilder() {
    }


    /**
     * Represents the implementation of nes.
     */
    public final class NesImpl implements Nes {

        private List<Ne> ne;

        @Override
        public List<Ne> ne() {
            return ne;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ne);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof NesImpl) {
                NesImpl other = (NesImpl) obj;
                return
                     Objects.equals(ne, other.ne);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("ne", ne)
                .toString();
        }

        /**
         * Creates an instance of nesImpl.
         *
         * @param builderObject builder object of nes
         */
        public NesImpl(NesBuilder builderObject) {
            this.ne = builderObject.ne();
        }
    }
}