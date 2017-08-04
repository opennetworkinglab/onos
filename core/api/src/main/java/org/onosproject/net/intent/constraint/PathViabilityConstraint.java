/*
 * Copyright 2017-present Open Networking Foundation
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

import org.onosproject.net.Link;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.ResourceContext;

/**
 * Abstract Constraint for constraints intended to influence
 * only path viability and not influence individual link cost
 * during path computation.
 */
public abstract class PathViabilityConstraint implements Constraint {

    @Override
    public final double cost(Link link, ResourceContext context) {
        // random value, should never be used.
        return 1.0;
    }

}
