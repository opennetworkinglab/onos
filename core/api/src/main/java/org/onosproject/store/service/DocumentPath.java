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

package org.onosproject.store.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * A path class for identifying nodes within the {@code DocumentTree}.
 *
 * Note: indexing is ONE based so the root is the 1st level, to retrieve the
 * root one should query level 1.
 */
public class DocumentPath implements Comparable {

    private final ArrayList<String> tokens = Lists.newArrayList();

    /**
     * Private utility constructor for internal generation of partial paths only.
     *
     * @param path
     */
    private DocumentPath(List<String> path) {
        Preconditions.checkNotNull(path);
        this.tokens.addAll(path);
    }

    /**
     * Constructor to generate new {@DocumentPath}, new paths must contain at
     * least one name and string names may NOT contain any '.'s. If one field
     * is null that field will be ignored.
     *
     * @throws IllegalDocumentNameException if both parameters are null or the string
     * name contains an illegal character ('.')
     * @param nodeName the name of the last level of this path
     * @param parentPath the path representing the parents leading up to this
     *                   node, in the case of the root this should be null
     */
    public DocumentPath(String nodeName, DocumentPath parentPath) {
        if (nodeName.contains(".")) {
            throw new IllegalDocumentNameException(
                    "Periods are not allowed in names.");
        }
        if (parentPath != null) {
            tokens.addAll(parentPath.path());
        }
        if (nodeName != null) {
            tokens.add(nodeName);
        }
        if (tokens.isEmpty()) {
            throw new IllegalDocumentNameException("A document path must contain at" +
                                                   "least one non-null" +
                                                   "element.");
        }
    }

    /**
     * Returns a path from the root to the parent of this node, if this node is
     * the root this call returns null.
     *
     * @return a {@code DocumentPath} representing a path to this paths parent,
     * null if this a root path
     */
    public DocumentPath parent() {
        if (tokens.size() <= 1) {
            return null;
        }
        return new DocumentPath(this.tokens.subList(0, tokens.size() - 1));
    }

    /**
     * Returns the complete list of tokens representing this path in correct
     * order.
     *
     * @return a list of strings representing this path
     */
    public List<String> path() {
        return ImmutableList.copyOf(tokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokens);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DocumentPath) {
            DocumentPath that = (DocumentPath) obj;
            return this.tokens.equals(that.tokens);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<String> iter = tokens.iterator();
        while (iter.hasNext()) {
            stringBuilder.append(iter.next());
            if (iter.hasNext()) {
                stringBuilder.append(".");
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof DocumentPath) {
            DocumentPath that = (DocumentPath) o;
            int shorterLength = this.tokens.size() > that.tokens.size() ?
                    that.tokens.size() : this.tokens.size();
            for (int i = 0; i < shorterLength; i++) {
                if (this.tokens.get(i).compareTo(that.tokens.get(i)) != 0) {
                    return this.tokens.get(i).compareTo(that.tokens.get(i));
                }
            }

            if (this.tokens.size() > that.tokens.size()) {
                return 1;
            } else if (that.tokens.size() > this.tokens.size()) {
                return -1;
            } else {
                return 0;
            }
        }
        throw new IllegalArgumentException("Compare can only compare objects" +
                                                   "of the same type.");
    }
}
