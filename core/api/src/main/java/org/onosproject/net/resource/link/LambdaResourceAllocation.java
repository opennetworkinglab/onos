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
package org.onosproject.net.resource.link;

import com.google.common.base.MoreObjects;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceType;

import java.util.Objects;

/**
 * Representation of allocated lambda resource.
 *
 * @deprecated in Emu Release
 */
@Deprecated
public class LambdaResourceAllocation implements ResourceAllocation {
    private final LambdaResource lambda;

    @Override
    public ResourceType type() {
        return ResourceType.LAMBDA;
    }

    /**
     * Creates a new {@link LambdaResourceAllocation} with {@link LambdaResource}
     * object.
     *
     * @param lambda allocated lambda
     */
    public LambdaResourceAllocation(LambdaResource lambda) {
        this.lambda = lambda;
    }

    /**
     * Returns the lambda resource.
     *
     * @return the lambda resource
     */
    public LambdaResource lambda() {
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
        final LambdaResourceAllocation other = (LambdaResourceAllocation) obj;
        return Objects.equals(this.lambda, other.lambda);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("lambda", lambda)
                .toString();
    }
}
