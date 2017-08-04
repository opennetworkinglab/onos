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

package org.onosproject.routing.bgp;

import com.google.common.base.MoreObjects;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import java.util.ArrayList;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a route in BGP.
 */
public class BgpRouteEntry extends RouteEntry {
    private final BgpSession bgpSession; // The BGP Session the route was
                                         // received on
    private final byte origin;          // Route ORIGIN: IGP, EGP, INCOMPLETE
    private final AsPath asPath;        // The AS Path
    private final long localPref;       // The local preference for the route
    private long multiExitDisc = BgpConstants.Update.MultiExitDisc.LOWEST_MULTI_EXIT_DISC;

    /**
     * Class constructor.
     *
     * @param bgpSession the BGP Session the route was received on
     * @param prefix the prefix of the route
     * @param nextHop the next hop of the route
     * @param origin the route origin: 0=IGP, 1=EGP, 2=INCOMPLETE
     * @param asPath the AS path
     * @param localPref the route local preference
     */
    public BgpRouteEntry(BgpSession bgpSession, IpPrefix prefix,
                         IpAddress nextHop, byte origin,
                         BgpRouteEntry.AsPath asPath, long localPref) {
        super(prefix, nextHop);
        this.bgpSession = checkNotNull(bgpSession);
        this.origin = origin;
        this.asPath = checkNotNull(asPath);
        this.localPref = localPref;
    }

    /**
     * Gets the BGP Session the route was received on.
     *
     * @return the BGP Session the route was received on
     */
    public BgpSession getBgpSession() {
        return bgpSession;
    }

    /**
     * Gets the route origin: 0=IGP, 1=EGP, 2=INCOMPLETE.
     *
     * @return the route origin: 0=IGP, 1=EGP, 2=INCOMPLETE
     */
    public byte getOrigin() {
        return origin;
    }

    /**
     * Gets the route AS path.
     *
     * @return the route AS path
     */
    public BgpRouteEntry.AsPath getAsPath() {
        return asPath;
    }

    /**
     * Gets the route local preference.
     *
     * @return the route local preference
     */
    public long getLocalPref() {
        return localPref;
    }

    /**
     * Gets the route MED (Multi-Exit Discriminator).
     *
     * @return the route MED (Multi-Exit Discriminator)
     */
    public long getMultiExitDisc() {
        return multiExitDisc;
    }

    /**
     * Sets the route MED (Multi-Exit Discriminator).
     *
     * @param multiExitDisc the route MED (Multi-Exit Discriminator) to set
     */
    void setMultiExitDisc(long multiExitDisc) {
        this.multiExitDisc = multiExitDisc;
    }

    /**
     * Tests whether the route is originated from the local AS.
     * <p>
     * The route is considered originated from the local AS if the AS Path
     * is empty or if it begins with an AS_SET (after skipping
     * AS_CONFED_SEQUENCE and AS_CONFED_SET).
     * </p>
     *
     * @return true if the route is originated from the local AS, otherwise
     * false
     */
    boolean isLocalRoute() {
        PathSegment firstPathSegment = null;

        // Find the first Path Segment by ignoring the AS_CONFED_* segments
        for (PathSegment pathSegment : asPath.getPathSegments()) {
            if ((pathSegment.getType() == BgpConstants.Update.AsPath.AS_SET) ||
                (pathSegment.getType() == BgpConstants.Update.AsPath.AS_SEQUENCE)) {
                firstPathSegment = pathSegment;
                break;
            }
        }
        if (firstPathSegment == null) {
            return true;                // Local route: no path segments
        }
        // If the first path segment is AS_SET, the route is considered local
        if (firstPathSegment.getType() == BgpConstants.Update.AsPath.AS_SET) {
            return true;
        }

        return false;                   // The route is not local
    }

    /**
     * Gets the BGP Neighbor AS number the route was received from.
     * <p>
     * If the router is originated from the local AS, the return value is
     * zero (BGP_AS_0).
     * </p>
     *
     * @return the BGP Neighbor AS number the route was received from.
     */
    long getNeighborAs() {
        PathSegment firstPathSegment = null;

        if (isLocalRoute()) {
            return BgpConstants.BGP_AS_0;
        }

        // Find the first Path Segment by ignoring the AS_CONFED_* segments
        for (PathSegment pathSegment : asPath.getPathSegments()) {
            if ((pathSegment.getType() == BgpConstants.Update.AsPath.AS_SET) ||
                (pathSegment.getType() == BgpConstants.Update.AsPath.AS_SEQUENCE)) {
                firstPathSegment = pathSegment;
                break;
            }
        }
        if (firstPathSegment == null) {
            // NOTE: Shouldn't happen - should be captured by isLocalRoute()
            return BgpConstants.BGP_AS_0;
        }

        if (firstPathSegment.getSegmentAsNumbers().isEmpty()) {
            // NOTE: Shouldn't happen. Should check during the parsing.
            return BgpConstants.BGP_AS_0;
        }
        return firstPathSegment.getSegmentAsNumbers().get(0);
    }

    /**
     * Tests whether the AS Path contains a loop.
     * <p>
     * The test is done by comparing whether the AS Path contains the
     * local AS number.
     * </p>
     *
     * @param localAsNumber the local AS number to compare against
     * @return true if the AS Path contains a loop, otherwise false
     */
    boolean hasAsPathLoop(long localAsNumber) {
        for (PathSegment pathSegment : asPath.getPathSegments()) {
            for (Long asNumber : pathSegment.getSegmentAsNumbers()) {
                if (asNumber.equals(localAsNumber)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Compares this BGP route against another BGP route by using the
     * BGP Decision Process.
     * <p>
     * NOTE: The comparison needs to be performed only on routes that have
     * same IP Prefix.
     * </p>
     *
     * @param other the BGP route to compare against
     * @return true if this BGP route is better than the other BGP route
     * or same, otherwise false
     */
    boolean isBetterThan(BgpRouteEntry other) {
        if (this == other) {
            return true;        // Return true if same route
        }

        // Compare the LOCAL_PREF values: larger is better
        if (getLocalPref() != other.getLocalPref()) {
            return (getLocalPref() > other.getLocalPref());
        }

        // Compare the AS number in the path: smaller is better
        if (getAsPath().getAsPathLength() !=
            other.getAsPath().getAsPathLength()) {
            return getAsPath().getAsPathLength() <
                other.getAsPath().getAsPathLength();
        }

        // Compare the Origin number: lower is better
        if (getOrigin() != other.getOrigin()) {
            return (getOrigin() < other.getOrigin());
        }

        // Compare the MED if the neighbor AS is same: larger is better
        medLabel: {
            if (isLocalRoute() || other.isLocalRoute()) {
                // Compare MEDs for non-local routes only
                break medLabel;
            }
            long thisNeighborAs = getNeighborAs();
            if (thisNeighborAs != other.getNeighborAs()) {
                break medLabel;             // AS number is different
            }
            if (thisNeighborAs == BgpConstants.BGP_AS_0) {
                break medLabel;             // Invalid AS number
            }

            // Compare the MED
            if (getMultiExitDisc() != other.getMultiExitDisc()) {
                return (getMultiExitDisc() > other.getMultiExitDisc());
            }
        }

        // Compare the peer BGP ID: lower is better
        Ip4Address peerBgpId = getBgpSession().remoteInfo().bgpId();
        Ip4Address otherPeerBgpId = other.getBgpSession().remoteInfo().bgpId();
        if (!peerBgpId.equals(otherPeerBgpId)) {
            return (peerBgpId.compareTo(otherPeerBgpId) < 0);
        }

        // Compare the peer BGP address: lower is better
        Ip4Address peerAddress = getBgpSession().remoteInfo().ip4Address();
        Ip4Address otherPeerAddress =
            other.getBgpSession().remoteInfo().ip4Address();
        if (!peerAddress.equals(otherPeerAddress)) {
            return (peerAddress.compareTo(otherPeerAddress) < 0);
        }

        return true;            // Routes are same. Shouldn't happen?
    }

    /**
     * A class to represent AS Path Segment.
     */
    public static class PathSegment {
        // Segment type: AS_SET(1), AS_SEQUENCE(2), AS_CONFED_SEQUENCE(3),
        // AS_CONFED_SET(4)
        private final byte type;
        private final ArrayList<Long> segmentAsNumbers;   // Segment AS numbers

        /**
         * Constructor.
         *
         * @param type the Path Segment Type: AS_SET(1), AS_SEQUENCE(2),
         * AS_CONFED_SEQUENCE(3), AS_CONFED_SET(4)
         * @param segmentAsNumbers the Segment AS numbers
         */
        PathSegment(byte type, ArrayList<Long> segmentAsNumbers) {
            this.type = type;
            this.segmentAsNumbers = checkNotNull(segmentAsNumbers);
        }

        /**
         * Gets the Path Segment Type: AS_SET(1), AS_SEQUENCE(2),
         * AS_CONFED_SEQUENCE(3), AS_CONFED_SET(4).
         *
         * @return the Path Segment Type: AS_SET(1), AS_SEQUENCE(2),
         * AS_CONFED_SEQUENCE(3), AS_CONFED_SET(4)
         */
        public byte getType() {
            return type;
        }

        /**
         * Gets the Path Segment AS Numbers.
         *
         * @return the Path Segment AS Numbers
         */
        public ArrayList<Long> getSegmentAsNumbers() {
            return segmentAsNumbers;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof PathSegment)) {
                return false;
            }

            PathSegment otherPathSegment = (PathSegment) other;
            return Objects.equals(this.type, otherPathSegment.type) &&
                Objects.equals(this.segmentAsNumbers,
                               otherPathSegment.segmentAsNumbers);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, segmentAsNumbers);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("type", BgpConstants.Update.AsPath.typeToString(type))
                .add("segmentAsNumbers", this.segmentAsNumbers)
                .toString();
        }
    }

    /**
     * A class to represent AS Path.
     */
    public static class AsPath {
        private final ArrayList<PathSegment> pathSegments;
        private final int asPathLength;         // Precomputed AS Path Length

        /**
         * Constructor.
         *
         * @param pathSegments the Path Segments of the Path
         */
         AsPath(ArrayList<PathSegment> pathSegments) {
             this.pathSegments = checkNotNull(pathSegments);

             //
             // Precompute the AS Path Length:
             // - AS_SET counts as 1
             // - AS_SEQUENCE counts how many AS numbers are included
             // - AS_CONFED_SEQUENCE and AS_CONFED_SET are ignored
             //
             int pl = 0;
             for (PathSegment pathSegment : pathSegments) {
                 switch (pathSegment.getType()) {
                 case BgpConstants.Update.AsPath.AS_SET:
                     pl++;              // AS_SET counts as 1
                     break;
                 case BgpConstants.Update.AsPath.AS_SEQUENCE:
                     // Count each AS number
                     pl += pathSegment.getSegmentAsNumbers().size();
                     break;
                 case BgpConstants.Update.AsPath.AS_CONFED_SEQUENCE:
                     break;             // Ignore
                 case BgpConstants.Update.AsPath.AS_CONFED_SET:
                     break;             // Ignore
                 default:
                     // NOTE: What to do if the Path Segment type is unknown?
                     break;
                 }
             }
             asPathLength = pl;
         }

        /**
         * Gets the AS Path Segments.
         *
         * @return the AS Path Segments
         */
        public ArrayList<PathSegment> getPathSegments() {
            return pathSegments;
        }

        /**
         * Gets the AS Path Length as considered by the BGP Decision Process.
         *
         * @return the AS Path Length as considered by the BGP Decision Process
         */
        int getAsPathLength() {
            return asPathLength;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof AsPath)) {
                return false;
            }

            AsPath otherAsPath = (AsPath) other;
            return Objects.equals(this.pathSegments, otherAsPath.pathSegments);
        }

        @Override
        public int hashCode() {
            return pathSegments.hashCode();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("pathSegments", this.pathSegments)
                .toString();
        }
    }

    /**
     * Compares whether two objects are equal.
     * <p>
     * NOTE: The bgpSession field is excluded from the comparison.
     * </p>
     *
     * @return true if the two objects are equal, otherwise false.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        //
        // NOTE: Subclasses are considered as change of identity, hence
        // equals() will return false if the class type doesn't match.
        //
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        if (!super.equals(other)) {
            return false;
        }

        // NOTE: The bgpSession field is excluded from the comparison
        BgpRouteEntry otherRoute = (BgpRouteEntry) other;
        return (this.origin == otherRoute.origin) &&
            Objects.equals(this.asPath, otherRoute.asPath) &&
            (this.localPref == otherRoute.localPref) &&
            (this.multiExitDisc == otherRoute.multiExitDisc);
    }

    /**
     * Computes the hash code.
     * <p>
     * NOTE: We return the base class hash code to avoid expensive computation
     * </p>
     *
     * @return the object hash code
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("prefix", prefix())
            .add("nextHop", nextHop())
            .add("bgpId", bgpSession.remoteInfo().bgpId())
            .add("origin", BgpConstants.Update.Origin.typeToString(origin))
            .add("asPath", asPath)
            .add("localPref", localPref)
            .add("multiExitDisc", multiExitDisc)
            .toString();
    }
}
