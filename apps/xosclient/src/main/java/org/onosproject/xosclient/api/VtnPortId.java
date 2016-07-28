/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.xosclient.api;

import com.google.common.base.Strings;
import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Representation of VTN port ID.
 */
public final class VtnPortId extends Identifier<String> {

    private VtnPortId(String id) {
        super(id);
    }

    /**
     * Returns vtn port identifier with value.
     *
     * @param id id
     * @return instance port id
     */
    public static VtnPortId of(String id) {
        checkArgument(!Strings.isNullOrEmpty(id), "VTN port ID cannot be null");
        return new VtnPortId(id);
    }
}
