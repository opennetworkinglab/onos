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
package org.onosproject.iptopology.api.link;

import com.google.common.base.MoreObjects;
import org.onosproject.iptopology.api.IpLinkIdentifier;
import org.onosproject.iptopology.api.LinkTed;
import org.onosproject.iptopology.api.TerminationPoint;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.SparseAnnotations;

/**
 * Default implementation of immutable ip link description entity.
 */
public class DefaultIpLinkDescription extends AbstractDescription
        implements IpLinkDescription {

    private final TerminationPoint src;
    private final TerminationPoint dst;
    private final IpLinkIdentifier linkIdentifier;
    private final LinkTed linkTed;

    /**
     * Creates an ip link description using the supplied information.
     *
     * @param src             link source
     * @param dst             link destination
     * @param linkIdentifier  link identifier
     * @param linkTed         link traffic engineering parameters
     * @param annotations optional key/value annotations
     */
    public DefaultIpLinkDescription(TerminationPoint src, TerminationPoint dst,
                                    IpLinkIdentifier linkIdentifier, LinkTed linkTed,
                                    SparseAnnotations... annotations) {
        super(annotations);
        this.src = src;
        this.dst = dst;
        this.linkIdentifier = linkIdentifier;
        this.linkTed = linkTed;
    }

    /**
     * Creates an ip link description using the supplied information.
     *
     * @param base        IpLinkDescription to basic information
     * @param annotations optional key/value annotations
     */
    public DefaultIpLinkDescription(IpLinkDescription base, SparseAnnotations... annotations) {
        this(base.src(), base.dst(), base.linkIdentifier(),
                base.linkTed(), annotations);
    }

    @Override
    public TerminationPoint src() {
        return src;
    }

    @Override
    public TerminationPoint dst() {
        return dst;
    }

    @Override
    public IpLinkIdentifier linkIdentifier() {
        return linkIdentifier;
    }

    @Override
    public LinkTed linkTed() {
        return linkTed; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("src", src())
                .add("dst", dst())
                .add("linkIdentifier", linkIdentifier())
                .add("linkTed", linkTed())
                .toString();
    }

}
