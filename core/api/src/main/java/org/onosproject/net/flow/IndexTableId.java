/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.flow;

import org.onlab.util.Identifier;

/**
 * Table identifier representing the position of the table in the pipeline.
 */
public final class IndexTableId extends Identifier<Integer> implements TableId {

    private IndexTableId(int index) {
        super(index);
    }

    @Override
    public Type type() {
        return Type.INDEX;
    }

    @Override
    public int compareTo(TableId other) {
        if (this.type() != other.type()) {
            return this.type().compareTo(other.type());
        } else {
            IndexTableId indexTableId = (IndexTableId) other;
            return this.id() - indexTableId.id();
        }
    }

    /**
     * Returns a table identifier for the given index.
     *
     * @param index table index
     * @return table identifier
     */
    public static IndexTableId of(int index) {
        return new IndexTableId(index);
    }
}
