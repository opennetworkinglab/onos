/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.core;

import com.google.common.base.MoreObjects;
import org.onlab.util.Identifier;

/**
 * Group identifier.
 */
public class GroupId extends Identifier<Integer> {

    public GroupId(int id) {
        super(id);
    }

    // Constructor for serialization
    private GroupId() {
        super(0);
    }

    /**
     * Returns a group ID as an integer value.
     * The method is not intended for use by application developers.
     * Return data type may change in the future release.
     *
     * @param id int value
     * @return group ID
     */
    public static GroupId valueOf(int id) {
        return new GroupId(id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", "0x" + Integer.toHexString(identifier))
                .toString();
    }
}
