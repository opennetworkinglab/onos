/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.ui.topo;

import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a link and its inverse, as a partial implementation.
 * <p>
 * Subclasses will decide how to generate the link highlighting (coloring
 * and labeling) for the topology view.
 */
public abstract class BiLink {

    private final LinkKey key;
    private final Link one;
    private Link two;

    /**
     * Constructs a bi-link for the given key and initial link. It is expected
     * that the caller will have used {@link TopoUtils#canonicalLinkKey(Link)}
     * to generate the key.
     *
     * @param key canonical key for this bi-link
     * @param link first link
     */
    public BiLink(LinkKey key, Link link) {
        this.key = checkNotNull(key);
        this.one = checkNotNull(link);
    }

    /**
     * Sets the second link for this bi-link.
     *
     * @param link second link
     */
    public void setOther(Link link) {
        this.two = checkNotNull(link);
    }

    /**
     * Returns the link identifier in the form expected on the Topology View
     * in the web client.
     *
     * @return link identifier
     */
    public String linkId() {
        return TopoUtils.compactLinkString(one);
    }

    /**
     * Returns the key for this bi-link.
     *
     * @return the key
     */
    public LinkKey key() {
        return key;
    }

    /**
     * Returns the first link in this bi-link.
     *
     * @return the first link
     */
    public Link one() {
        return one;
    }

    /**
     * Returns the second link in this bi-link.
     *
     * @return the second link
     */
    public Link two() {
        return two;
    }

    /**
     * Returns the link highlighting to use, based on this bi-link's current
     * state.
     *
     * @param type optional highlighting type parameter
     * @return link highlighting model
     */
    public abstract LinkHighlight highlight(Enum<?> type);
}
