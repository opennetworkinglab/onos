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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf
            .importroutes;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype.BgpcommImRouteProtocol;

/**
 * Represents the builder implementation of importRoute.
 */
public class ImportRouteBuilder implements ImportRoute.ImportRouteBuilder {

    private BgpcommImRouteProtocol importProtocol;
    private String importProcessId;

    @Override
    public BgpcommImRouteProtocol importProtocol() {
        return importProtocol;
    }

    @Override
    public String importProcessId() {
        return importProcessId;
    }

    @Override
    public ImportRouteBuilder importProtocol(BgpcommImRouteProtocol importProtocol) {
        this.importProtocol = importProtocol;
        return this;
    }

    @Override
    public ImportRouteBuilder importProcessId(String importProcessId) {
        this.importProcessId = importProcessId;
        return this;
    }

    @Override
    public ImportRoute build() {
        return new ImportRouteImpl(this);
    }

    /**
     * Creates an instance of importRouteBuilder.
     */
    public ImportRouteBuilder() {
    }


    /**
     * Represents the implementation of importRoute.
     */
    public final class ImportRouteImpl implements ImportRoute {

        private BgpcommImRouteProtocol importProtocol;
        private String importProcessId;

        @Override
        public BgpcommImRouteProtocol importProtocol() {
            return importProtocol;
        }

        @Override
        public String importProcessId() {
            return importProcessId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(importProtocol, importProcessId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ImportRouteImpl) {
                ImportRouteImpl other = (ImportRouteImpl) obj;
                return
                     Objects.equals(importProtocol, other.importProtocol) &&
                     Objects.equals(importProcessId, other.importProcessId);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("importProtocol", importProtocol)
                .add("importProcessId", importProcessId)
                .toString();
        }

        /**
         * Creates an instance of importRouteImpl.
         *
         * @param builderObject builder object of importRoute
         */
        public ImportRouteImpl(ImportRouteBuilder builderObject) {
            this.importProtocol = builderObject.importProtocol();
            this.importProcessId = builderObject.importProcessId();
        }
    }
}