/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.newresource;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An object that is used to locate a resource in a network.
 * A ResourcePath represents a path that is hierarchical and composed of a sequence
 * of elementary resources that are not globally identifiable. A ResourcePath can be a globally
 * unique resource identifier.
 *
 * Users of this class must keep the semantics of resources regarding the hierarchical structure.
 * For example, resource path, Link:1/VLAN ID:100, is valid, but resource path, VLAN ID:100/Link:1
 * is not valid because a link is not a sub-component of a VLAN ID.
 */
@Beta
public final class ResourcePath {

    private final ResourcePath parent;
    private final Object last;

    public static final ResourcePath ROOT = new ResourcePath(ImmutableList.of());

    /**
     * Creates an resource path from the specified components.
     *
     * @param components components of the path. The order represents hierarchical structure of the resource.
     */
    public ResourcePath(Object... components) {
        this(Arrays.asList(components));
    }

    /**
     * Creates an resource path from the specified components.
     *
     * @param components components of the path. The order represents hierarchical structure of the resource.
     */
    public ResourcePath(List<Object> components) {
        checkNotNull(components);
        if (components.isEmpty()) {
            this.parent = null;
            this.last = null;
            return;
        }

        LinkedList<Object> children = new LinkedList<>(components);
        this.last = children.pollLast();
        this.parent = new ResourcePath(children);
    }

    /**
     * Creates an resource path from the specified parent and child.
     *
     * @param parent the parent of this resource
     * @param last a child of the parent
     */
    public ResourcePath(ResourcePath parent, Object last) {
        this.parent = checkNotNull(parent);
        this.last = checkNotNull(last);
    }

    // for serialization
    private ResourcePath() {
        this.parent = null;
        this.last = null;
    }

    /**
     * Returns the components of this resource path.
     *
     * @return the components of this resource path
     */
    public List<Object> components() {
        LinkedList<Object> components = new LinkedList<>();

        ResourcePath parentPath = parent;
        while (parentPath != null) {
            components.addFirst(last);
            parentPath = parent.parent;
        }

        return components;
    }

    /**
     * Returns the parent resource path of this instance.
     * E.g. if this path is Link:1/VLAN ID:100, the return value is the resource path for Link:1.
     *
     * @return the parent resource path of this instance.
     * If there is no parent, empty instance will be returned.
     */
    public Optional<ResourcePath> parent() {
        return Optional.ofNullable(parent);
    }

    public ResourcePath child(Object child) {
        return new ResourcePath(this, child);
    }

    /**
     * Returns the last component of this instance.
     *
     * @return the last component of this instance.
     * The return value is equal to the last object of {@code components()}.
     */
    public Object last() {
        return last;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.parent, this.last);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ResourcePath)) {
            return false;
        }
        final ResourcePath that = (ResourcePath) obj;
        return Objects.equals(this.parent, that.parent)
                && Objects.equals(this.last, that.last);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("parent", parent)
                .add("last", last)
                .toString();
    }
}
