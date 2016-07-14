/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.net.intent.constraint;


import org.onosproject.net.EncapsulationType;
import org.onosproject.net.Link;
import org.onosproject.net.intent.ResourceContext;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encapsulation to manage core transportation.
 */
public class EncapsulationConstraint extends BooleanConstraint {

    private EncapsulationType encapType;

    /**
     * Creates a new encapsulation constraint.
     *
     * @param encapType the encapsulation type {@link EncapsulationType}
     */
    public EncapsulationConstraint(EncapsulationType encapType) {
        checkNotNull(encapType, "EncapsulationType cannot be null");
        this.encapType = encapType;
    }


    // doesn't use LinkResourceService
    @Override
    public boolean isValid(Link link, ResourceContext context) {
        //TODO: validate the availability of the resources for each link in the path.
        //e.g., availability of MPLSlabels, VLANID

        return true;
    }

    /**
     * Returns the encapsulation type required by this constraint.
     *
     * @return encapType
     */
    public EncapsulationType encapType() {
        return encapType;
    }

    @Override
    public int hashCode() {
        return encapType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EncapsulationConstraint other = (EncapsulationConstraint) obj;
        return this.encapType() == other.encapType();
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("encapType", encapType).toString();
    }
}
