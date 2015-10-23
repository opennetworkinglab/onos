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
package org.onosproject.ovsdb.controller;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * The class representing an ovsdb bridge.
 * This class is immutable.
 */
public final class OvsdbBridge {

    private final OvsdbBridgeName bridgeName;
    private final OvsdbDatapathId datapathId;

    /**
     * Constructor from an OvsdbBridgeName bridgeName and an OvsdbDatapathId
     * datapathId.
     *
     * @param bridgeName the bridgeName to use
     * @param datapathId the datapathId to use
     */
    public OvsdbBridge(OvsdbBridgeName bridgeName, OvsdbDatapathId datapathId) {
        checkNotNull(bridgeName, "bridgeName is not null");
        checkNotNull(datapathId, "datapathId is not null");
        this.bridgeName = bridgeName;
        this.datapathId = datapathId;
    }

    /**
     * Gets the bridge name of bridge.
     *
     * @return the bridge name of bridge
     */
    public OvsdbBridgeName bridgeName() {
        return bridgeName;
    }

    /**
     * Gets the datapathId of bridge.
     *
     * @return datapathId the datapathId to use
     */
    public OvsdbDatapathId datapathId() {
        return datapathId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bridgeName, datapathId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OvsdbBridge) {
            final OvsdbBridge otherOvsdbBridge = (OvsdbBridge) obj;
            return Objects.equals(this.bridgeName, otherOvsdbBridge.bridgeName)
                    && Objects.equals(this.datapathId,
                                      otherOvsdbBridge.datapathId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("bridgeName", bridgeName.value())
                .add("datapathId", datapathId.value()).toString();
    }
}
