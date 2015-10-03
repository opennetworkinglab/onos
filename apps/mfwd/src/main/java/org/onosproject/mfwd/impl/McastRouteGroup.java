/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.mfwd.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.HashMap;
import org.onlab.packet.IpPrefix;

/**
 * The McastRouteGroup extends the McastRouteBase class and serves two purposes:
 * first it represents a (*, G) multicast route entry.  Second it serves
 * as a container for all (S, G) multicast route entries that belong
 * to the same group address.
 */
public class McastRouteGroup extends McastRouteBase {
    private HashMap<IpPrefix, McastRouteSource> sources;

    /**
     * Class constructor.
     *
     * @param gaddr - String representation of group address.
     */
    public McastRouteGroup(String gaddr) {
        super(checkNotNull(gaddr));
        this.init();
    }

    /**
     * Create a multicast group.
     *
     * @param gpfx - Group address
     */
    public McastRouteGroup(IpPrefix gpfx) {
        super(checkNotNull(gpfx));
        this.init();
    }

    /**
     * Common initialization used by constructors.
     */
    private void init() {
        this.sources = new HashMap();
        super.isGroup = true;
    }

    /**
     * Find a specific multicast source address for this group.
     *
     * @param saddr the source address
     * @return the multicast source route or null if it does not exist
     */
    public McastRouteSource findSource(IpPrefix saddr) {
        return this.sources.get(checkNotNull(saddr));
    }

    /**
     * Return the entire set of multicast sources for this group.
     *
     * @return the set of multicast sources
     */
    public HashMap<IpPrefix, McastRouteSource> getSources() {
        return this.sources;
    }

    /**
     * Add a new McastRouteSource to this group.
     *
     * @param src the multicast source
     */
    public void addSource(McastRouteSource src) {
        checkNotNull(src);
        this.sources.put(src.getSaddr(), src);
    }

    /**
     * Remove the source with this specific IpPrefix from this group entry.
     *
     * @param spfx IP Prefix of the source to be removed
     * @return the source route that was just removed
     */
    public McastRouteSource removeSource(IpPrefix spfx) {
        McastRouteSource src = this.sources.remove(spfx);
        src.withdrawIntent();
        return src;
    }

    /**
     * Remove all sources from this.
     */
    public void removeSources() {
        for (McastRouteSource src : this.sources.values()) {
            src.withdrawIntent();
            this.sources.remove(src.getSaddr());
        }
    }

}
