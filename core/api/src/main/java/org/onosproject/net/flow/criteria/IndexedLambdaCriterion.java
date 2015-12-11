/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.flow.criteria;

import com.google.common.base.MoreObjects;
import org.onosproject.net.IndexedLambda;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of indexed lambda criterion.
 *
 * @deprecated in Emu (ONOS 1.4).
 */
@Deprecated
public class IndexedLambdaCriterion implements Criterion {

    private final IndexedLambda lambda;

    /**
     * Creates a criterion with the specified value.
     *
     * @param lambda lambda index number
     */
    IndexedLambdaCriterion(IndexedLambda lambda) {
        this.lambda = checkNotNull(lambda);
    }

    @Override
    public Type type() {
        // TODO: consider defining a new specific type
        // Now OCH_SIGID is used due to compatibility concerns
        return Type.OCH_SIGID;
    }

    /**
     * Returns the indexed lambda to match.
     *
     * @return the indexed lambda to match
     */
    public IndexedLambda lambda() {
        return lambda;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), lambda);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IndexedLambdaCriterion)) {
            return false;
        }
        final IndexedLambdaCriterion that = (IndexedLambdaCriterion) obj;
        return Objects.equals(this.lambda, that.lambda);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("lambda", lambda)
                .toString();
    }
}
