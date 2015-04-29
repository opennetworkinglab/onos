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
package org.onosproject.net;

import com.google.common.base.MoreObjects;

/**
 * Implementation of Lambda simply designated by an index number of wavelength.
 */
public class IndexedLambda implements Lambda {

    private final long lambda;

    /**
     * Creates an instance representing the wavelength specified by the given index number.
     *
     * @param lambda index number of wavelength
     */
    IndexedLambda(long lambda) {
        this.lambda = lambda;
    }

    @Override
    public int hashCode() {
        return (int) (lambda ^ (lambda >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IndexedLambda)) {
            return false;
        }

        final IndexedLambda that = (IndexedLambda) obj;
        return this.lambda == that.lambda;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("lambda", lambda)
                .toString();
    }
}
