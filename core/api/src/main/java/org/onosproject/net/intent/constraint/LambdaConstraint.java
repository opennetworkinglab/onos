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
package org.onosproject.net.intent.constraint;

import com.google.common.annotations.Beta;
import org.onosproject.net.IndexedLambda;
import org.onosproject.net.Link;
import org.onosproject.net.resource.link.LinkResourceService;
import org.onosproject.net.resource.ResourceRequest;
import org.onosproject.net.resource.ResourceType;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Constraint that evaluates links based on available lambda.
 */
@Beta
public class LambdaConstraint extends BooleanConstraint {

    private final IndexedLambda lambda;

    /**
     * Creates a new optical lambda constraint.
     *
     * @param lambda optional lambda to indicate a specific lambda
     */
    public LambdaConstraint(IndexedLambda lambda) {
        this.lambda = lambda;
    }

    // Constructor for serialization
    private LambdaConstraint() {
        this.lambda = null;
    }

    @Override
    public boolean isValid(Link link, LinkResourceService resourceService) {
        for (ResourceRequest request : resourceService.getAvailableResources(link)) {
            if (request.type() == ResourceType.LAMBDA) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the lambda required by this constraint.
     *
     * @return required lambda
     */
    public IndexedLambda lambda() {
        return lambda;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(lambda);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final LambdaConstraint other = (LambdaConstraint) obj;
        return Objects.equals(this.lambda, other.lambda);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("lambda", lambda).toString();
    }
}
