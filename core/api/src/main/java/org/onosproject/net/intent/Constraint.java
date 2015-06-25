/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.intent;

import com.google.common.annotations.Beta;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.resource.link.LinkResourceService;

/**
 * Representation of a connectivity constraint capable of evaluating a link
 * and determining the cost of traversing that link in the context of this
 * constraint.
 */
@Beta
public interface Constraint {

    // TODO: Consider separating cost vs viability.

    /**
     * Evaluates the specified link and provides the cost for its traversal.
     *
     * @param link            link to be evaluated
     * @param resourceService resource service for validating availability of
     *                        link resources
     * @return cost of link traversal
     */
    double cost(Link link, LinkResourceService resourceService);

    /**
     * Validates that the specified path satisfies the constraint.
     *
     * @param path            path to be validated
     * @param resourceService resource service for validating availability of
     *                        link resources
     * @return cost of link traversal
     */
    boolean validate(Path path, LinkResourceService resourceService);

}
