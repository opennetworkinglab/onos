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
package org.onosproject.net.resource;

import java.util.Objects;

/**
 * Representation of lambda resource.
 */
public final class Lambda extends LinkResource {

    private final int lambda;

    /**
     * Creates a new instance with given lambda.
     *
     * @param lambda lambda value to be assigned
     */
    private Lambda(int lambda) {
        this.lambda = lambda;
    }

    // Constructor for serialization
    private Lambda() {
        this.lambda = 0;
    }

    /**
     * Creates a new instance with given lambda.
     *
     * @param lambda lambda value to be assigned
     * @return {@link Lambda} instance with given lambda
     */
    public static Lambda valueOf(int lambda) {
        return new Lambda(lambda);
    }

    /**
     * Returns lambda as an int value.
     *
     * @return lambda as an int value
     */
    public int toInt() {
        return lambda;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Lambda) {
            Lambda that = (Lambda) obj;
            return Objects.equals(this.lambda, that.lambda);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.lambda);
    }

    @Override
    public String toString() {
        return String.valueOf(this.lambda);
    }

}
