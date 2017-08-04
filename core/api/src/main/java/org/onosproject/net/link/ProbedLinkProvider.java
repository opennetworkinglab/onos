/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.link;

import com.google.common.hash.HashFunction;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import org.onosproject.cluster.ClusterMetadata;

/**
 * Abstraction for an entity that provides information about infrastructure
 * links that are discovered or verified using probe messages.
 */
public interface ProbedLinkProvider extends LinkProvider {

    static final String DEFAULT_MAC = "DE:AD:BE:EF:BA:11";

    static String defaultMac() {
        return DEFAULT_MAC;
    }

    /**
     * Build a stringified MAC address using the ClusterMetadata hash for uniqueness.
     * Form of MAC is "02:eb" followed by four bytes of clusterMetadata hash.
     *
     * @param cm cluster metadata
     * @return stringified mac address
     */
    static String fingerprintMac(ClusterMetadata cm) {
        if (cm == null) {
            return DEFAULT_MAC;
        }

        HashFunction hf = Hashing.murmur3_32();
        HashCode hc = hf.newHasher().putObject(cm, ClusterMetadata.HASH_FUNNEL).hash();
        int unqf = hc.asInt();

        StringBuilder sb = new StringBuilder();
        sb.append("02:eb");
        for (int i = 0; i < 4; i++) {
            byte b = (byte) (unqf >> i * 8);
            sb.append(String.format(":%02X", b));
        }
        return sb.toString();
    }
}
