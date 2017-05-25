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

import com.google.common.annotations.Beta;
import org.onlab.util.Identifier;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifier of a table in a protocol-independent pipeline.
 */
@Beta
public final class PiTableId extends Identifier<String> {

    private final String scope;
    private final String name;

    /**
     * Creates a new table identifier for the given scope and table name.
     *
     * @param scope table scope
     * @param name  table name
     */
    public PiTableId(String scope, String name) {
        super(checkNotNull(scope) + '.' + checkNotNull(name));
        this.scope = scope;
        this.name = name;
    }

    /**
     * Creates a new table identifier for the given table name.
     *
     * @param name table name
     */
    public PiTableId(String name) {
        super(checkNotNull(name));
        this.name = name;
        this.scope = null;
    }


    /**
     * Returns the name of this table.
     *
     * @return table name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the scope of this table, if present.
     *
     * @return optional scope
     */
    public Optional<String> scope() {
        return Optional.ofNullable(scope);
    }


}
