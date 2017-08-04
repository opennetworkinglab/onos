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
package org.onosproject.ovsdb.rfc.operations;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.ovsdb.rfc.schema.TableSchema;

/**
 * comment operation.Refer to RFC 7047 Section 5.2.
 */
public final class Comment implements Operation {

    private final String op;
    private final String comment;

    /**
     * Constructs a Comment object.
     * @param comment the comment member of comment operation
     */
    public Comment(String comment) {
        checkNotNull(comment, "comment cannot be null");
        this.op = Operations.COMMENT.op();
        this.comment = comment;
    }

    /**
     * Returns the comment member of comment operation.
     * @return the comment member of comment operation
     */
    public String getComment() {
        return comment;
    }

    @Override
    public String getOp() {
        return op;
    }

    @Override
    public TableSchema getTableSchema() {
        return null;
    }
}
