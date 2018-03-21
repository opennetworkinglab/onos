/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.store.service;

import org.onosproject.store.primitives.DistributedPrimitiveOptions;

/**
 * Builder for {@link DocumentTree}.
 */
public abstract class DocumentTreeOptions<O extends DocumentTreeOptions<O, V>, V>
        extends DistributedPrimitiveOptions<O> {

    private boolean purgeOnUninstall = false;
    private Ordering ordering = Ordering.NATURAL;

    public DocumentTreeOptions() {
        super(DistributedPrimitive.Type.DOCUMENT_TREE);
    }

    /**
     * Clears document tree contents when the owning application is uninstalled.
     *
     * @return this builder
     */
    public O withPurgeOnUninstall() {
        purgeOnUninstall = true;
        return (O) this;
    }

    /**
     * Return if document tree entries need to be cleared when owning application is uninstalled.
     *
     * @return true if items are to be cleared on uninstall
     */
    public boolean purgeOnUninstall() {
        return purgeOnUninstall;
    }

    /**
     * Sets the ordering of the tree nodes.
     * <p>
     * When {@link AsyncDocumentTree#getChildren(DocumentPath)} is called, children will be returned according to
     * the specified sort order.
     *
     * @param ordering ordering of the tree nodes
     * @return this builder
     */
    public O withOrdering(Ordering ordering) {
        this.ordering = ordering;
        return (O) this;
    }

    /**
     * Returns the ordering of tree nodes.
     * <p>
     * When {@link AsyncDocumentTree#getChildren(DocumentPath)} is called, children will be returned according to
     * the specified sort order.
     *
     * @return the ordering of tree nodes
     */
    public Ordering ordering() {
        return ordering;
    }
}