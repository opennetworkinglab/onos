/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.routing.fpm;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents an FPM peer with accept routes flag.
 */
public class FpmPeerAcceptRoutes {

    private final boolean isAcceptRoutes;
    private final FpmPeer peer;



    /**
     * Creates a new FPM peer.
     *
     * @param peer Fpm Peer
     * @param isAcceptRoutes is route accepted on peer
     *
     */
    public FpmPeerAcceptRoutes(FpmPeer peer, boolean isAcceptRoutes) {
        this.peer = peer;
        this.isAcceptRoutes = isAcceptRoutes;
    }

    /**
     * Returns isAcceptRoutes flag status.
     *
     * @return isAcceptRoutes
     */
    public boolean isAcceptRoutes() {
        return isAcceptRoutes;
    }

    /**
     * Returns the FPM peer.
     *
     * @return FPM peer
     */
    public FpmPeer peer() {
        return peer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isAcceptRoutes, peer);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof FpmPeerAcceptRoutes)) {
            return false;
        }

        FpmPeerAcceptRoutes that = (FpmPeerAcceptRoutes) other;

        return Objects.equals(this.isAcceptRoutes, that.isAcceptRoutes) &&
                Objects.equals(this.peer, that.peer);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("peer", peer)
                .add("isAcceptRoutes", isAcceptRoutes)
                .toString();
    }

}
