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
import org.onosproject.net.flow.TableId;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifier of a table in a protocol-independent pipeline.
 */
@Beta
public final class PiTableId extends Identifier<String> implements TableId  {

    private final String scope;
    private final String name;

    private PiTableId(String scope, String name) {
        super((scope != null ? scope + "." : "") + name);
        this.scope = scope;
        this.name = name;
    }

    /**
     * Returns a table identifier for the given table scope and name.
     *
     * @param scope table scope
     * @param name  table name
     * @return table identifier
     */
    public static PiTableId of(String scope, String name) {
        checkNotNull(name);
        checkNotNull(scope);
        checkArgument(!name.isEmpty(), "Name can't be empty");
        checkArgument(!scope.isEmpty(), "Scope can't be empty");
        return new PiTableId(scope, name);
    }

    /**
     * Returns a table identifier for the given table name.
     *
     * @param name table name
     * @return table identifier
     */
    public static PiTableId of(String name) {
        checkNotNull(name);
        checkArgument(!name.isEmpty(), "Name can't be empty");
        return new PiTableId(null, name);
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

    @Override
    public Type type() {
        return Type.PIPELINE_INDEPENDENT;
    }
}
