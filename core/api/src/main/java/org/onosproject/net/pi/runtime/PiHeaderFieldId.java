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

package org.onosproject.net.pi.runtime;

import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifier of a packet's header field.
 */
public final class PiHeaderFieldId extends Identifier<String> {

    private final String headerName;
    private final String fieldName;
    private final int index;

    private PiHeaderFieldId(String headerName, String fieldName, int index) {
        super(headerName +
                      (index > 0 ? "[" + String.valueOf(index) + "]" : "") +
                      "." + fieldName);
        this.headerName = headerName;
        this.fieldName = fieldName;
        this.index = index;
    }

    /**
     * Returns an header field identifier for the given header name, field name and index.
     * <p>
     * Index represents the position of this header in the packet w.r.t. to other headers of the
     * same type. Index 0 points to the first instance of the header, 1 the second one, etc. Helpful
     * when dealing with stacked headers, e.g. to match on the second MPLS label.
     *
     * @param headerName header name
     * @param fieldName  field name
     * @param index      index
     * @return header field identifier
     */
    public static PiHeaderFieldId of(String headerName, String fieldName, int index) {
        checkNotNull(headerName);
        checkNotNull(fieldName);
        checkArgument(!headerName.isEmpty(), "Header name can't be empty");
        checkArgument(!fieldName.isEmpty(), "Field name can't be empty");
        checkArgument(index >= 0, "Index must be a positive integer");
        return new PiHeaderFieldId(headerName, fieldName, index);
    }

    /**
     * Returns an header field identifier for the given header name and field name.
     * Index is set to default value 0.
     *
     * @param headerName header name
     * @param fieldName  field name
     * @return header field identifier
     */
    public static PiHeaderFieldId of(String headerName, String fieldName) {
        return of(headerName, fieldName, 0);
    }

    /**
     * Returns the name of the header.
     *
     * @return a string value
     */
    public String headerName() {
        return headerName;
    }

    /**
     * Returns the name of the field.
     *
     * @return a string value
     */
    public String fieldName() {
        return fieldName;
    }

    /**
     * Returns the index of this header.
     *
     * @return an integer value.
     */
    public int index() {
        return index;
    }
}
