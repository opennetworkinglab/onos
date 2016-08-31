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

package org.onosproject.store.primitives;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import org.onosproject.store.service.DocumentPath;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A tree node made for {@code DocumentTree}
 * implementations that keeps records of its parent and children.
 */
public class DocumentTreeNode<V> {
    private final DocumentPath key;
    private V value;
    private final TreeSet<DocumentTreeNode<V>> children =
            Sets.newTreeSet(new Comparator<DocumentTreeNode<V>>() {
                @Override
                public int compare(DocumentTreeNode<V> o1,
                                   DocumentTreeNode<V> o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });
    private DocumentTreeNode parent;

    public DocumentTreeNode(DocumentPath key, V value,
                            DocumentTreeNode parent) {
        this.key = checkNotNull(key);
        this.value = checkNotNull(value);
        this.parent = parent;
    }

    /**
     * Returns this objects key.
     *
     * @return the key
     */
    public DocumentPath getKey() {
        return key;
    }

    /**
     * Returns this objects value.
     *
     * @return the value
     */
    public V getValue() {
        return value;
    }

    /**
     * Sets this objects value.
     *
     * @param value the value to be set
     */
    public void setValue(V value) {
        this.value = value;
    }

    /**
     * Returns a collection of the children of this node.
     *
     * @return a sorted iterator for the children of this node.
     */
    public Iterator<DocumentTreeNode<V>> getChildren() {
        return children.iterator();
    }

    /**
     * Adds a child to this node.
     *
     * @param child the child node to be added
     * @return true if the child set was modified as a result of this call,
     * false otherwise
     */
    public boolean addChild(DocumentTreeNode<V> child) {
        return children.add(child);
    }

    /**
     * Removes a child from the children of this node.
     *
     * @param child the child node to be removed
     * @return true if the child set was modified as a result of this call,
     * false otherwise
     */
    public boolean removeChild(String child) {
        return children.remove(child);
    }

    /**
     * Returns the parent of this node.
     *
     * @return the parent node of this node, which may be null
     */
    public DocumentTreeNode<V> getParent() {
        return parent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DocumentTreeNode) {
            DocumentTreeNode that = (DocumentTreeNode) obj;
            if (this.parent.equals(that.parent)) {
                if (this.children.size() == that.children.size()) {
                    for (DocumentTreeNode child : this.children) {
                        if (!that.children.contains(child)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper =
                MoreObjects.toStringHelper(getClass())
                .add("parent", this.parent)
                .add("key", this.key)
                .add("value", this.value);
        for (DocumentTreeNode child : children) {
            helper = helper.add("child", child.key);
        }
        return helper.toString();
    }

}
