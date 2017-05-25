/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.pi.model;

import com.google.common.annotations.Beta;

/**
 * Model of a header instance in a protocol-independent pipeline.
 */
@Beta
public interface PiHeaderModel {
    /**
     * Returns the type of this header instance.
     *
     * @return a header type value
     */
    PiHeaderTypeModel type();

    /**
     * Returns true if this header instance is a metadata, false elsewhere.
     *
     * @return a boolean value
     */
    boolean isMetadata();

    /**
     * Returns the index of this header w.r.t. to other headers of the same type.
     * Index 0 points to the first instance of the header, 1 the second one, etc.
     * Helpful when dealing with stacked headers. e.g. to match on the second MPLS label.
     *
     * @return a non-negative integer value
     */
    default int index() {
        return 0;
    }
}
