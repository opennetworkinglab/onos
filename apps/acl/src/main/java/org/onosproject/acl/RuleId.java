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
 *
 * Originally created by Pengfei Lu, Network and Cloud Computing Laboratory, Dalian University of Technology, China
 * Advisers: Keqiu Li and Heng Qi
 * This work is supported by the State Key Program of National Natural Science of China(Grant No. 61432002)
 * and Prospective Research Project on Future Networks in Jiangsu Future Networks Innovation Institute.
 */
package org.onosproject.acl;

import org.onlab.util.Identifier;

/**
 * ACL rule identifier suitable as an external key.
 * <p>This class is immutable.</p>
 */
public final class RuleId extends Identifier<Long> {
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
        super(0L);
    }

    /**
     * Constructs the ID corresponding to a given long value.
     *
     * @param value the underlying value of this ID
     */
    RuleId(long value) {
        super(value);
    }

    /**
     * Returns the backing value.
     *
     * @return the value
     */
    public long fingerprint() {
        return identifier;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(identifier);
    }
}
