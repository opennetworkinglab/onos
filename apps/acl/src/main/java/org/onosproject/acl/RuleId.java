/*
 * Copyright 2015 Open Networking Laboratory
 * Originally created by Pengfei Lu, Network and Cloud Computing Laboratory, Dalian University of Technology, China
 * Advisers: Keqiu Li and Heng Qi
 * This work is supported by the State Key Program of National Natural Science of China(Grant No. 61432002)
 * and Prospective Research Project on Future Networks in Jiangsu Future Networks Innovation Institute.
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
package org.onosproject.acl;

/**
 * ACL rule identifier suitable as an external key.
 * <p>This class is immutable.</p>
 */
public final class RuleId {
    private final long value;

    /**
     * Creates an ACL rule identifier from the specified long value.
     *
     * @param value long value
     * @return ACL rule identifier
     */
    public static RuleId valueOf(long value) {
        return new RuleId(value);
    }

    /**
     * Constructor for serializer.
     */
    RuleId() {
        this.value = 0;
    }

    /**
     * Constructs the ID corresponding to a given long value.
     *
     * @param value the underlying value of this ID
     */
    RuleId(long value) {
        this.value = value;
    }

    /**
     * Returns the backing value.
     *
     * @return the value
     */
    public long fingerprint() {
        return value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof RuleId)) {
            return false;
        }
        RuleId that = (RuleId) obj;
        return this.value == that.value;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(value);
    }
}
