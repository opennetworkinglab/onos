/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onosproject.net.link.LinkDescription;

import com.google.common.base.MoreObjects;

// TODO Consider renaming.
// it's an identifier for a Link, but it's not ElementId, so not using LinkId.

/**
 * Immutable representation of a link identity.
 */
public final class LinkKey {

    private final ConnectPoint src;
    private final ConnectPoint dst;

    /**
     * Returns source connection point.
     *
     * @return source connection point
     */
    public ConnectPoint src() {
        return src;
    }

    /**
     * Returns destination connection point.
     *
     * @return destination connection point
     */
    public ConnectPoint dst() {
        return dst;
    }

    /**
     * Creates a link identifier with source and destination connection point.
     *
     * @param src source connection point
     * @param dst destination connection point
     */
    private LinkKey(ConnectPoint src, ConnectPoint dst) {
        this.src = checkNotNull(src);
        this.dst = checkNotNull(dst);
    }

    /**
     * Creates a link identifier with source and destination connection point.
     *
     * @param src source connection point
     * @param dst destination connection point
     * @return a link identifier
     */
    public static LinkKey linkKey(ConnectPoint src, ConnectPoint dst) {
        return new LinkKey(src, dst);
    }

    /**
     * Creates a link identifier for the specified link.
     *
     * @param link link descriptor
     * @return a link identifier
     */
    public static LinkKey linkKey(Link link) {
        return new LinkKey(link.src(), link.dst());
    }

    /**
     * Creates a link identifier for the specified link.
     *
     * @param link {@link Description}
     * @return a link identifier
     */
    public static LinkKey linkKey(LinkDescription link) {
        return new LinkKey(link.src(), link.dst());
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LinkKey) {
            final LinkKey other = (LinkKey) obj;
            return Objects.equals(this.src, other.src) &&
                    Objects.equals(this.dst, other.dst);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("src", src)
                .add("dst", dst)
                .toString();
    }
}
