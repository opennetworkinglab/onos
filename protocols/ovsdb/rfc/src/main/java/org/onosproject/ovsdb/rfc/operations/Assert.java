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
 * assert operation.Refer to RFC 7047 Section 5.2.
 */
public final class Assert implements Operation {

    private final String op;
    private final String lock;

    /**
     * Constructs a Assert object.
     * @param lock the lock member of assert operation
     */
    public Assert(String lock) {
        checkNotNull(lock, "lock cannot be null");
        this.op = Operations.ASSERT.op();
        this.lock = lock;
    }

    /**
     * Returns the lock member of assert operation.
     * @return the lock member of assert operation
     */
    public String getLock() {
        return lock;
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
