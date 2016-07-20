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
package org.onosproject.iptopology.api.device;

import com.google.common.base.MoreObjects;
import org.onosproject.iptopology.api.PrefixIdentifier;
import org.onosproject.iptopology.api.PrefixTed;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.SparseAnnotations;

/**
 * Default implementation of immutable Prefix description.
 */
public class DefaultPrefixDescription extends AbstractDescription
        implements PrefixDescription {

    private final PrefixIdentifier prefixIdentifier;
    private final PrefixTed prefixTed;


    /**
     * Creates prefix description using the supplied information.
     *
     * @param prefixIdentifier prefix identifier
     * @param prefixTed        prefix traffic engineering parameters
     * @param annotations      optional key/value annotations map
     */
    public DefaultPrefixDescription(PrefixIdentifier prefixIdentifier, PrefixTed prefixTed,
                                  SparseAnnotations...annotations) {
        super(annotations);
        this.prefixIdentifier = prefixIdentifier;
        this.prefixTed = prefixTed;
    }

    /**
     * Default constructor for serialization.
     */
    private DefaultPrefixDescription() {
        this.prefixIdentifier = null;
        this.prefixTed = null;
    }

    /**
     * Creates prefix description using the supplied information.
     *
     * @param base        PrefixDescription to get basic information from
     * @param annotations optional key/value annotations map
     */
    public DefaultPrefixDescription(PrefixDescription base,
                                  SparseAnnotations annotations) {
        this(base.prefixIdentifier(), base.prefixTed(), annotations);
    }

    @Override
    public PrefixIdentifier prefixIdentifier() {
        return prefixIdentifier;
    }

    @Override
    public PrefixTed prefixTed() {
        return prefixTed;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("prefixIdentifier", prefixIdentifier)
                .add("prefixTed", prefixTed)
                .add("annotations", annotations())
                .toString();
    }

}
