/*
 * Copyright 2015-present Open Networking Laboratory
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
 * commit operation.Refer to RFC 7047 Section 5.2.
 */
public final class Commit implements Operation {

    private final String op;
    private final Boolean durable;

    /**
     * Constructs a Commit object.
     * @param durable the durable member of commit operation
     */
    public Commit(Boolean durable) {
        checkNotNull(durable, "durable cannot be null");
        this.op = Operations.COMMIT.op();
        this.durable = durable;
    }

    /**
     * Returns the durable member of commit operation.
     * @return the durable member of commit operation
     */
    public Boolean isDurable() {
        return durable;
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
