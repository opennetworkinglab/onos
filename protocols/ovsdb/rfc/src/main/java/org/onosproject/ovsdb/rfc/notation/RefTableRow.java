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
 */
package org.onosproject.ovsdb.rfc.notation;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The RefTable type that can be expanded to Row. Refer to RFC 7047 Section 3.2.
 */
public final class RefTableRow {

    private final String refTable;
    private final JsonNode jsonNode;

    /**
     * RefTableRow constructor.
     * @param refTable the refTable value of JsonNode
     * @param jsonNode JsonNode
     */
    public RefTableRow(String refTable, JsonNode jsonNode) {
        checkNotNull(refTable, "refTable cannot be null");
        checkNotNull(jsonNode, "jsonNode cannot be null");
        this.refTable = refTable;
        this.jsonNode = jsonNode;
    }

    /**
     * Returns JsonNode.
     * @return JsonNode
     */
    public JsonNode jsonNode() {
        return jsonNode;
    }

    /**
     * Returns refTable.
     * @return refTable
     */
    public String refTable() {
        return refTable;
    }

    @Override
    public int hashCode() {
        return Objects.hash(refTable, jsonNode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RefTableRow) {
            final RefTableRow other = (RefTableRow) obj;
            return Objects.equals(this.refTable, other.refTable)
                    && Objects.equals(this.jsonNode, other.jsonNode);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("refTable", refTable)
                .add("jsonNode", jsonNode).toString();
    }
}
