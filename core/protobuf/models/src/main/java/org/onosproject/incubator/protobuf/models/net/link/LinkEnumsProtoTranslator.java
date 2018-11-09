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
package org.onosproject.incubator.protobuf.models.net.link;

import org.onosproject.grpc.net.link.models.LinkEnumsProto;
import org.onosproject.net.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * gRPC LinkType and LinkState message to equivalent ONOS enum conversion related utilities.
 */
public final class LinkEnumsProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(LinkEnumsProtoTranslator.class);

    /**
     * Translates gRPC enum LinkType to Optional of ONOS enum.
     *
     * @param type linktype type in gRPC enum
     * @return Optional of equivalent ONOS enum or empty if not recognized
     */
    public static Optional<Link.Type> translate(LinkEnumsProto.LinkTypeProto type) {
        switch (type) {
            case DIRECT:
                return Optional.of(Link.Type.DIRECT);
            case INDIRECT:
                return Optional.of(Link.Type.INDIRECT);
            case EDGE:
                return Optional.of(Link.Type.EDGE);
            case TUNNEL:
                return Optional.of(Link.Type.TUNNEL);
            case OPTICAL:
                return Optional.of(Link.Type.OPTICAL);
            case VIRTUAL:
                return Optional.of(Link.Type.VIRTUAL);
            default:
                log.warn("Unrecognized Type gRPC message: {}", type);
                return Optional.empty();
        }
    }

    /**
     * Translates ONOS enum Type to gRPC enum.
     *
     * @param type ONOS' Type type
     * @return equivalent gRPC message enum
     */
    public static LinkEnumsProto.LinkTypeProto translate(Link.Type type) {
        switch (type) {
            case DIRECT:
                return LinkEnumsProto.LinkTypeProto.DIRECT;
            case INDIRECT:
                return LinkEnumsProto.LinkTypeProto.INDIRECT;
            case EDGE:
                return LinkEnumsProto.LinkTypeProto.EDGE;
            case TUNNEL:
                return LinkEnumsProto.LinkTypeProto.TUNNEL;
            case OPTICAL:
                return LinkEnumsProto.LinkTypeProto.OPTICAL;
            case VIRTUAL:
                return LinkEnumsProto.LinkTypeProto.VIRTUAL;
            default:
                log.warn("Unrecognized type", type);
                throw new IllegalArgumentException("Unrecognized Type");
        }
    }

    /**
     * Translates gRPC enum LinkState to Optional of ONOS enum.
     *
     * @param state linkstate state in gRPC enum
     * @return Optional of equivalent ONOS enum or empty if not recognized
     */
    public static Optional<Link.State> translate(LinkEnumsProto.LinkStateProto state) {
        switch (state) {
            case ACTIVE:
                return Optional.of(Link.State.ACTIVE);
            case INACTIVE:
                return Optional.of(Link.State.INACTIVE);
            default:
                log.warn("Unrecognized State gRPC message: {}", state);
                return Optional.empty();
        }
    }

    /**
     * Translates ONOS enum State to gRPC enum.
     *
     * @param state ONOS' state state
     * @return equivalent gRPC message enum
     */
    public static LinkEnumsProto.LinkStateProto translate(Link.State state) {
        switch (state) {
            case ACTIVE:
                return LinkEnumsProto.LinkStateProto.ACTIVE;
            case INACTIVE:
                return LinkEnumsProto.LinkStateProto.INACTIVE;
            default:
                log.warn("Unrecognized State", state);
                throw new IllegalArgumentException("Unrecognized State");
        }
    }


    // Utility class not intended for instantiation.
    private LinkEnumsProtoTranslator() {}

}

