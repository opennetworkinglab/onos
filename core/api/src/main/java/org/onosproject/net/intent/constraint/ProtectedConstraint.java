/*
 * Copyright 2016-present Open Networking Foundation
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

import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Link;
import org.onosproject.net.intent.ResourceContext;

/**
 * Constraint to request using only {@link AnnotationKeys#PROTECTED protected}
 * Links.
 */
public class ProtectedConstraint extends BooleanConstraint {

    private static final ProtectedConstraint PROTECTED_CONSTRAINT
            = new ProtectedConstraint();

    /**
     * Returns {@link ProtectedConstraint} instance.
     *
     * @return {@link ProtectedConstraint} instance
     */
    public static ProtectedConstraint useProtectedLink() {
        return PROTECTED_CONSTRAINT;
    }

    @Override
    public boolean isValid(Link link, ResourceContext context) {
        return link.annotations().keys().contains(AnnotationKeys.PROTECTED);
    }

    @Override
    public String toString() {
        return "Protected";
    }

}
