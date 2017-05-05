/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.google.common.annotations.Beta;

/**
 * This constraint is a flag and tells the compiler that it is allowed to generate
 * {@link org.onosproject.net.domain.DomainIntent}.
 */
@Beta
public class DomainConstraint extends MarkerConstraint {

    private static final DomainConstraint DOMAIN_CONSTRAINT =
            new DomainConstraint();

    protected DomainConstraint() {
    }

    /**
     * Returns domain constraint.
     *
     * @return domain constraint
     */
    public static DomainConstraint domain() {
        return DOMAIN_CONSTRAINT;
    }
}
