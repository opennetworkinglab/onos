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
package org.onosproject.net.link;

import com.google.common.base.MoreObjects;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.SparseAnnotations;
import com.google.common.base.Objects;

/**
 * Default implementation of immutable link description entity.
 */
public class DefaultLinkDescription extends AbstractDescription
        implements LinkDescription {

    private final ConnectPoint src;
    private final ConnectPoint dst;
    private final Link.Type type;
    private final boolean isExpected;

    public static final boolean EXPECTED = true;
    public static final boolean NOT_EXPECTED = false;

    /**
     * Creates a link description using the supplied information.
     *
     * @param src         link source
     * @param dst         link destination
     * @param type        link type
     * @param isExpected  is the link expected to be part of this configuration
     * @param annotations optional key/value annotations
     */
    public DefaultLinkDescription(ConnectPoint src, ConnectPoint dst,
                                  Link.Type type,
                                  boolean isExpected,
                                  SparseAnnotations... annotations) {
        super(annotations);
        this.src = src;
        this.dst = dst;
        this.type = type;
        this.isExpected = isExpected;
    }

    /**
     * Creates a link description using the supplied information.
     *
     * @param src         link source
     * @param dst         link destination
     * @param type        link type
     * @param annotations optional key/value annotations
     */
    public DefaultLinkDescription(ConnectPoint src, ConnectPoint dst,
                                  Link.Type type,
                                  SparseAnnotations... annotations) {
        this(src, dst, type, EXPECTED, annotations);
    }

    @Override
    public ConnectPoint src() {
        return src;
    }

    @Override
    public ConnectPoint dst() {
        return dst;
    }

    @Override
    public Link.Type type() {
        return type;
    }

    @Override
    public boolean isExpected() {
        return isExpected;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("src", src())
                .add("dst", dst())
                .add("type", type())
                .add("isExpected", isExpected())
                .add("annotations", annotations())
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), src, dst, type, isExpected);
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && getClass() == object.getClass()) {
            if (!super.equals(object)) {
                return false;
            }
            DefaultLinkDescription that = (DefaultLinkDescription) object;
            return Objects.equal(this.src, that.src)
                    && Objects.equal(this.dst, that.dst)
                    && Objects.equal(this.type, that.type)
                    && Objects.equal(this.isExpected, that.isExpected);
        }
        return false;
    }

}
