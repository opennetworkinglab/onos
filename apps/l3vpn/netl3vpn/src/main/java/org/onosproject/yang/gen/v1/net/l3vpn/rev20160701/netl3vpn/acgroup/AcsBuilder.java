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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs.Ac;

/**
 * Represents the builder implementation of acs.
 */
public class AcsBuilder implements Acs.AcsBuilder {

    private List<Ac> ac;

    @Override
    public List<Ac> ac() {
        return ac;
    }

    @Override
    public AcsBuilder ac(List<Ac> ac) {
        this.ac = ac;
        return this;
    }

    @Override
    public Acs build() {
        return new AcsImpl(this);
    }

    /**
     * Creates an instance of acsBuilder.
     */
    public AcsBuilder() {
    }


    /**
     * Represents the implementation of acs.
     */
    public final class AcsImpl implements Acs {

        private List<Ac> ac;

        @Override
        public List<Ac> ac() {
            return ac;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ac);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof AcsImpl) {
                AcsImpl other = (AcsImpl) obj;
                return
                     Objects.equals(ac, other.ac);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("ac", ac)
                .toString();
        }

        /**
         * Creates an instance of acsImpl.
         *
         * @param builderObject builder object of acs
         */
        public AcsImpl(AcsBuilder builderObject) {
            this.ac = builderObject.ac();
        }
    }
}