/*
 * Copyright 2015-present Open Networking Foundation
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

import java.util.Objects;

/**
 * Implementation of lambda (wavelength) criterion (16 bits unsigned
 * integer).
 */
public final class LambdaCriterion implements Criterion {
    private static final int MASK = 0xffff;
    private final int lambda;               // Lambda value: 16 bits
    private final Type type;

    /**
     * Constructor.
     *
     * @param lambda the lambda (wavelength) to match (16 bits unsigned
     * integer)
     * @param type the match type. Should be Type.OCH_SIGID
     */
    LambdaCriterion(int lambda, Type type) {
        this.lambda = lambda & MASK;
        this.type = type;
    }

    @Override
    public Type type() {
        return this.type;
    }

    /**
     * Gets the lambda (wavelength) to match.
     *
     * @return the lambda (wavelength) to match (16 bits unsigned integer)
     */
    public int lambda() {
        return lambda;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + lambda;
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
        if (obj instanceof LambdaCriterion) {
            LambdaCriterion that = (LambdaCriterion) obj;
            return Objects.equals(lambda, that.lambda) &&
                    Objects.equals(type, that.type);
        }
        return false;
    }
}
