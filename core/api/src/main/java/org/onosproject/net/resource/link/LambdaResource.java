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

import org.onosproject.net.IndexedLambda;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of lambda resource.
 *
 * @deprecated in Emu Release
 */
@Deprecated
public final class LambdaResource implements LinkResource {

    private final IndexedLambda lambda;

    /**
     * Creates a new instance with given lambda.
     *
     * @param lambda lambda to be assigned
     */
    private LambdaResource(IndexedLambda lambda) {
        this.lambda = checkNotNull(lambda);
    }

    // Constructor for serialization
    private LambdaResource() {
        this.lambda = null;
    }

    /**
     * Creates a new instance with the given index of lambda.
     *
     * @param lambda index value of lambda to be assigned
     * @return {@link LambdaResource} instance with the given lambda
     */
    public static LambdaResource valueOf(int lambda) {
        return valueOf(new IndexedLambda(lambda));
    }

    /**
     * Creates a new instance with the given lambda.
     *
     * @param lambda lambda to be assigned
     * @return {@link LambdaResource} instance with the given lambda
     */
    public static LambdaResource valueOf(IndexedLambda lambda) {
        return new LambdaResource(lambda);
    }

    /**
     * Returns lambda as an int value.
     *
     * @return lambda as an int value
     */
    public int toInt() {
        return (int) lambda.index();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LambdaResource) {
            LambdaResource that = (LambdaResource) obj;
            return Objects.equals(this.lambda, that.lambda);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return lambda.hashCode();
    }

    @Override
    public String toString() {
        return String.valueOf(this.lambda);
    }

}
