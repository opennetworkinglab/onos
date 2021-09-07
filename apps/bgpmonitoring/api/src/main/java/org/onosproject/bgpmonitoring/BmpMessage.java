/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.bgpmonitoring;

import org.onlab.packet.bmp.Bmp;
import org.onlab.packet.bmp.BmpPeer;

/**
 * Abstraction of an entity providing BMP Messages.
 */
public interface BmpMessage {

    /**
     * Returns BMP Header of BMP Message.
     *
     * @return BMP Header of BMP Message
     */
    Bmp getHeader();

    /**
     * Returns BMP Peer Header of BMP Message.
     *
     * @return BMP Peer Header of BMP Message
     */
    default BmpPeer getPeerHeader() {
        throw new UnsupportedOperationException("Bmp Per Peer Header not supported");
    }

    /**
     * Is bmp message has peer header.
     *
     * @return true if bmp has peer header otherwise false
     */
    default boolean hasPeerHeader() {
        return true;
    }

    /**
     * Returns version of BMP Message.
     *
     * @return version of BMP Message
     */
    default BmpVersion getVersion() {
        return BmpVersion.BMP_3;
    }

    /**
     * Returns BMP Type of BMP Message.
     *
     * @return BMP Type of BMP Message
     */
    BmpType getType();
}
