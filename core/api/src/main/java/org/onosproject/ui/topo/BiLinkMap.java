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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a collection of {@link BiLink} concrete classes. These maps
 * are used to collate a set of unidirectional {@link Link}s into a smaller
 * set of bi-directional {@link BiLink} derivatives.
 * <p>
 * @param <B> the type of bi-link subclass
 */
public abstract class BiLinkMap<B extends BiLink> {

    private final Map<LinkKey, B> map = new HashMap<>();

    /**
     * Creates a new instance of a bi-link. Concrete subclasses should
     * instantiate and return the appropriate bi-link subclass.
     *
     * @param key the link key
     * @param link the initial link
     * @return a new instance
     */
    protected abstract B create(LinkKey key, Link link);

    /**
     * Adds the given link to our collection, returning the corresponding
     * bi-link (creating one if needed necessary).
     *
     * @param link the link to add to the collection
     * @return the corresponding bi-link wrapper
     */
    public B add(Link link) {
        LinkKey key = TopoUtils.canonicalLinkKey(checkNotNull(link));
        B blink = map.get(key);
        if (blink == null) {
            // no bi-link yet exists for this link
            blink = create(key, link);
            map.put(key, blink);
        } else {
            // we have a bi-link for this link.
            if (!blink.one().equals(link)) {
                blink.setOther(link);
            }
        }
        return blink;
    }

    /**
     * Returns the bi-link instances in the collection.
     *
     * @return the bi-links in this map
     */
    public Collection<B> biLinks() {
        return map.values();
    }

    /**
     * Returns the number of bi-links in the collection.
     *
     * @return number of bi-links
     */
    public int size() {
        return map.size();
    }
}
