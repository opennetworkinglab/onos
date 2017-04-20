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
package org.onosproject.store.primitives.impl;

import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.DocumentTreeBuilder;

/**
 * Default {@link AsyncDocumentTree} builder.
 *
 * @param <V> type for document tree value
 */
public class DefaultDocumentTreeBuilder<V> extends DocumentTreeBuilder<V> {

    private final DistributedPrimitiveCreator primitiveCreator;

    public DefaultDocumentTreeBuilder(DistributedPrimitiveCreator primitiveCreator) {
        this.primitiveCreator = primitiveCreator;
    }

    @Override
    public AsyncDocumentTree<V> buildDocumentTree() {
        return primitiveCreator.newAsyncDocumentTree(name(), serializer());
    }

    //TODO
    /*
     public ConsistentDocumentTree<V> build() {
        return buildDocumentTree().asDocumentTree();
    }
    }*/
    //writing a dummy implementation till we have ConsistentDocumentTree.
    @Deprecated
    @Override
    public AsyncDocumentTree<V> build() {
        return primitiveCreator.newAsyncDocumentTree(name(), serializer(), executorSupplier());
    }
}