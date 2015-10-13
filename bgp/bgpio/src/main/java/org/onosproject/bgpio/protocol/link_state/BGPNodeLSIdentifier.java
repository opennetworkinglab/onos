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
package org.onosproject.bgpio.protocol.link_state;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.types.BGPErrorType;
import org.onosproject.bgpio.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Implementation of Node Identifier which includes local node descriptor/remote node descriptors.
 */
public class BGPNodeLSIdentifier {

    protected static final Logger log = LoggerFactory.getLogger(BGPNodeLSIdentifier.class);
    private NodeDescriptors nodeDescriptors;

    /**
     * Resets fields.
     */
    public BGPNodeLSIdentifier() {
        this.nodeDescriptors = null;
    }

    /**
     * Constructor to initialize fields.
     *
     * @param nodeDescriptors local/remote node descriptor
     */
    public BGPNodeLSIdentifier(NodeDescriptors nodeDescriptors) {
        this.nodeDescriptors = nodeDescriptors;
    }

    /**
     * Parse local node descriptors.
     *
     * @param cb ChannelBuffer
     * @param protocolId protocol identifier
     * @return object of this BGPNodeLSIdentifier
     * @throws BGPParseException while parsing local node descriptors
     */
    public static BGPNodeLSIdentifier parseLocalNodeDescriptors(ChannelBuffer cb, byte protocolId)
            throws BGPParseException {
        ChannelBuffer tempBuf = cb;
        short type = cb.readShort();
        short length = cb.readShort();
        if (cb.readableBytes() < length) {
            throw new BGPParseException(BGPErrorType.UPDATE_MESSAGE_ERROR, BGPErrorType.OPTIONAL_ATTRIBUTE_ERROR,
                                        tempBuf.readBytes(cb.readableBytes() + Constants.TYPE_AND_LEN));
        }
        NodeDescriptors nodeDescriptors = new NodeDescriptors();
        ChannelBuffer tempCb = cb.readBytes(length);

        if (type == NodeDescriptors.LOCAL_NODE_DES_TYPE) {
            nodeDescriptors = NodeDescriptors.read(tempCb, length, type, protocolId);
        } else {
            throw new BGPParseException(BGPErrorType.UPDATE_MESSAGE_ERROR, BGPErrorType.MALFORMED_ATTRIBUTE_LIST, null);
        }
        return new BGPNodeLSIdentifier(nodeDescriptors);
    }

    /**
     * Returns node descriptors.
     *
     * @return node descriptors
     */
    public NodeDescriptors getNodedescriptors() {
        return this.nodeDescriptors;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BGPNodeLSIdentifier) {
            BGPNodeLSIdentifier other = (BGPNodeLSIdentifier) obj;
            return Objects.equals(nodeDescriptors, other.nodeDescriptors);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeDescriptors);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("NodeDescriptors", nodeDescriptors)
                .toString();
    }
}