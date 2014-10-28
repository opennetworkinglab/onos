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
package org.onlab.onos.net.intent;

import org.onlab.onos.net.flow.BatchOperationTarget;

/**
 * Intent identifier suitable as an external key.
 * <p/>
 * This class is immutable.
 */
public final class IntentId implements BatchOperationTarget {

    private final long fingerprint;

    /**
     * Creates an intent identifier from the specified string representation.
     *
     * @param fingerprint long value
     * @return intent identifier
     */
    public static IntentId valueOf(long fingerprint) {
        return new IntentId(fingerprint);
    }

    /**
     * Constructor for serializer.
     */
    IntentId() {
        this.fingerprint = 0;
    }

    /**
     * Constructs the ID corresponding to a given long value.
     *
     * @param fingerprint the underlying value of this ID
     */
    IntentId(long fingerprint) {
        this.fingerprint = fingerprint;
    }

    @Override
    public int hashCode() {
        return (int) (fingerprint ^ (fingerprint >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof IntentId)) {
            return false;
        }
        IntentId that = (IntentId) obj;
        return this.fingerprint == that.fingerprint;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(fingerprint);
    }

}
