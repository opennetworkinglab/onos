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
package org.onosproject.iptopology.api;

import org.onosproject.net.AbstractModel;
import org.onosproject.net.Annotations;
import org.onosproject.net.provider.ProviderId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * This class provides Link identifier and link ted details.
 */
public class DefaultIpLink extends AbstractModel implements IpLink {

    private final TerminationPoint src;
    private final TerminationPoint dst;
    private final IpLinkIdentifier linkIdentifier;
    private final LinkTed linkTed;

    /**
     * Constructor to initialize its parameters.
     *
     * @param providerId     provider identification
     * @param src            link source termination point
     * @param dst            link destination termination point
     * @param linkIdentifier provides link identifier details
     * @param linkTed        provides link traffic engineering details
     * @param annotations    optional key/value annotations
     */
    public DefaultIpLink(ProviderId providerId, TerminationPoint src, TerminationPoint dst,
                         IpLinkIdentifier linkIdentifier, LinkTed linkTed,
                         Annotations... annotations) {
        super(providerId, annotations);
        this.src = src;
        this.dst = dst;
        this.linkIdentifier = linkIdentifier;
        this.linkTed = linkTed;
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
        return linkTed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst, linkIdentifier, linkTed);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultIpLink) {
            final DefaultIpLink other = (DefaultIpLink) obj;
            return Objects.equals(this.src, other.src) &&
                    Objects.equals(this.dst, other.dst) &&
                    Objects.equals(this.linkIdentifier, other.linkIdentifier) &&
                    Objects.equals(this.linkTed, other.linkTed);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .omitNullValues()
                .add("src", src)
                .add("dst", dst)
                .add("linkIdentifier", linkIdentifier)
                .add("linkTed", linkTed)
                .toString();
    }
}