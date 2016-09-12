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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Unique key for nodes in the {@link DocumentTree}.
 */
public class DocumentPath implements Comparable<DocumentPath> {

    private final List<String> pathElements = Lists.newArrayList();

    /**
     * Private utility constructor for internal generation of partial paths only.
     *
     * @param pathElements list of path elements
     */
    private DocumentPath(List<String> pathElements) {
        Preconditions.checkNotNull(pathElements);
        this.pathElements.addAll(pathElements);
    }

    /**
     * Constructs a new document path.
     * <p>
     * New paths must contain at least one name and string names may NOT contain any period characters.
     * If one field is {@code null} that field will be ignored.
     *
     * @param nodeName the name of the last level of this path
     * @param parentPath the path representing the parent leading up to this
     *                   node, in the case of the root this should be {@code null}
     * @throws IllegalDocumentNameException if both parameters are null or name contains an illegal character ('.')
     */
    public DocumentPath(String nodeName, DocumentPath parentPath) {
        if (nodeName.contains(".")) {
            throw new IllegalDocumentNameException(
                    "Periods are not allowed in names.");
        }
        if (parentPath != null) {
            pathElements.addAll(parentPath.pathElements());
        }
        if (nodeName != null) {
            pathElements.add(nodeName);
        }
        if (pathElements.isEmpty()) {
            throw new IllegalDocumentNameException("A document path must contain at" +
                                                   "least one non-null" +
                                                   "element.");
        }
    }

    /**
     * Creates a new {@code DocumentPath} from a period delimited path string.
     *
     * @param path path string
     * @return {@code DocumentPath} instance
     */
    public static DocumentPath from(String path) {
        return new DocumentPath(Arrays.asList(path.split("\\.")));
    }

    /**
     * Returns a path for the parent of this node.
     *
     * @return parent node path. If this path is for the root, returns {@code null}.
     */
    public DocumentPath parent() {
        if (pathElements.size() <= 1) {
            return null;
        }
        return new DocumentPath(this.pathElements.subList(0, pathElements.size() - 1));
    }

    /**
     * Returns the list of path elements representing this path in correct
     * order.
     *
     * @return a list of elements that make up this path
     */
    public List<String> pathElements() {
        return ImmutableList.copyOf(pathElements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathElements);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DocumentPath) {
            DocumentPath that = (DocumentPath) obj;
            return this.pathElements.equals(that.pathElements);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<String> iter = pathElements.iterator();
        while (iter.hasNext()) {
            stringBuilder.append(iter.next());
            if (iter.hasNext()) {
                stringBuilder.append(".");
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public int compareTo(DocumentPath that) {
        int shorterLength = this.pathElements.size() > that.pathElements.size()
                ? that.pathElements.size() : this.pathElements.size();
        for (int i = 0; i < shorterLength; i++) {
            if (this.pathElements.get(i).compareTo(that.pathElements.get(i)) != 0) {
                return this.pathElements.get(i).compareTo(that.pathElements.get(i));
            }
        }

        if (this.pathElements.size() > that.pathElements.size()) {
            return 1;
        } else if (that.pathElements.size() > this.pathElements.size()) {
            return -1;
        } else {
            return 0;
        }
    }
}
