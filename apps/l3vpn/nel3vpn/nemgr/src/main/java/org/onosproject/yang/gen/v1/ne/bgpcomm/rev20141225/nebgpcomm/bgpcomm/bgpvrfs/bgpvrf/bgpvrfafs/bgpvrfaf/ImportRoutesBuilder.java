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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf
            .importroutes.ImportRoute;

/**
 * Represents the builder implementation of importRoutes.
 */
public class ImportRoutesBuilder implements ImportRoutes.ImportRoutesBuilder {

    private List<ImportRoute> importRoute;

    @Override
    public List<ImportRoute> importRoute() {
        return importRoute;
    }

    @Override
    public ImportRoutesBuilder importRoute(List<ImportRoute> importRoute) {
        this.importRoute = importRoute;
        return this;
    }

    @Override
    public ImportRoutes build() {
        return new ImportRoutesImpl(this);
    }

    /**
     * Creates an instance of importRoutesBuilder.
     */
    public ImportRoutesBuilder() {
    }


    /**
     * Represents the implementation of importRoutes.
     */
    public final class ImportRoutesImpl implements ImportRoutes {

        private List<ImportRoute> importRoute;

        @Override
        public List<ImportRoute> importRoute() {
            return importRoute;
        }

        @Override
        public int hashCode() {
            return Objects.hash(importRoute);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ImportRoutesImpl) {
                ImportRoutesImpl other = (ImportRoutesImpl) obj;
                return
                     Objects.equals(importRoute, other.importRoute);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("importRoute", importRoute)
                .toString();
        }

        /**
         * Creates an instance of importRoutesImpl.
         *
         * @param builderObject builder object of importRoutes
         */
        public ImportRoutesImpl(ImportRoutesBuilder builderObject) {
            this.importRoute = builderObject.importRoute();
        }
    }
}