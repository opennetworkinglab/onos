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

package org.onosproject.segmentrouting;

import java.util.List;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default Tunnel class.
 */
public class DefaultTunnel implements Tunnel {

    private final String id;
    private final List<Integer> labelIds;

    private int groupId;
    private boolean allowedToRemoveGroup;

    /**
     * Creates a Tunnel reference.
     *
     * @param tid  Tunnel ID
     * @param labelIds Label stack of the tunnel
     */
    public DefaultTunnel(String tid, List<Integer> labelIds) {
        this.id = checkNotNull(tid);
        this.labelIds = labelIds;
        //TODO: need to register the class in Kryo for this
        //this.labelIds = Collections.unmodifiableList(labelIds);
        this.groupId = -1;
    }

    /**
     * Creates a new DefaultTunnel reference using the tunnel reference.
     *
     * @param tunnel DefaultTunnel reference
     */
    public DefaultTunnel(DefaultTunnel tunnel) {
        this.id = tunnel.id;
        this.labelIds = tunnel.labelIds;
        this.groupId = tunnel.groupId;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public List<Integer> labelIds() {
        return this.labelIds;
    }

    @Override
    public int groupId() {
        return this.groupId;
    }

    @Override
    public void setGroupId(int id) {
        this.groupId = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof DefaultTunnel) {
            DefaultTunnel tunnel = (DefaultTunnel) o;
            // We compare only the tunnel paths.
            if (tunnel.labelIds.equals(this.labelIds)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return labelIds.hashCode();
    }

    @Override
    public boolean isAllowedToRemoveGroup() {
        return this.allowedToRemoveGroup;
    }

    @Override
    public void allowToRemoveGroup(boolean b) {
        this.allowedToRemoveGroup = b;
    }
}
