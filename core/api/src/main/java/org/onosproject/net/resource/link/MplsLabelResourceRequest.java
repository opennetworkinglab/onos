/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.net.resource.link;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import org.onosproject.net.resource.ResourceRequest;
import org.onosproject.net.resource.ResourceType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a request for lambda resource.
 *
 * @deprecated in Emu Release
 */
@Deprecated
public class MplsLabelResourceRequest implements ResourceRequest {

    private final MplsLabel mplsLabel;

    /**
     * Constructs a request specifying the given MPLS label.
     *
     * @param mplsLabel MPLS label to be requested
     */
    @Beta
    public MplsLabelResourceRequest(MplsLabel mplsLabel) {
        this.mplsLabel = checkNotNull(mplsLabel);
    }

    /**
     * Constructs a request asking an arbitrary available MPLS label.
     *
     * @deprecated in Emu Release
     */
    @Deprecated
    public MplsLabelResourceRequest() {
        this.mplsLabel = null;
    }

    /**
     * Returns the MPLS label this request expects.
     *
     * @return the MPLS label this request expects
     */
    @Beta
    public MplsLabel mplsLabel() {
        return mplsLabel;
    }

    @Override
    public ResourceType type() {
        return ResourceType.MPLS_LABEL;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mplsLabel", mplsLabel)
                .toString();
    }
}
