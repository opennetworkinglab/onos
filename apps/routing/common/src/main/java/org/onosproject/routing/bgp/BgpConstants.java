/*
 * Copyright 2017-present Open Networking Laboratory
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

/**
 * BGP related constants.
 */
public final class BgpConstants {
    /**
     * Default constructor.
     * <p>
     * The constructor is private to prevent creating an instance of
     * this utility class.
     */
    private BgpConstants() {
    }

    /** BGP port number (RFC 4271). */
    public static final int BGP_PORT = 179;

    /** BGP version. */
    public static final int BGP_VERSION = 4;

    /** BGP OPEN message type. */
    public static final int BGP_TYPE_OPEN = 1;

    /** BGP UPDATE message type. */
    public static final int BGP_TYPE_UPDATE = 2;

    /** BGP NOTIFICATION message type. */
    public static final int BGP_TYPE_NOTIFICATION = 3;

    /** BGP KEEPALIVE message type. */
    public static final int BGP_TYPE_KEEPALIVE = 4;

    /** BGP Header Marker field length. */
    public static final int BGP_HEADER_MARKER_LENGTH = 16;

    /** BGP Header length. */
    public static final int BGP_HEADER_LENGTH = 19;

    /** BGP message maximum length. */
    public static final int BGP_MESSAGE_MAX_LENGTH = 4096;

    /** BGP OPEN message minimum length (BGP Header included). */
    public static final int BGP_OPEN_MIN_LENGTH = 29;

    /** BGP UPDATE message minimum length (BGP Header included). */
    public static final int BGP_UPDATE_MIN_LENGTH = 23;

    /** BGP NOTIFICATION message minimum length (BGP Header included). */
    public static final int BGP_NOTIFICATION_MIN_LENGTH = 21;

    /** BGP KEEPALIVE message expected length (BGP Header included). */
    public static final int BGP_KEEPALIVE_EXPECTED_LENGTH = 19;

    /** BGP KEEPALIVE messages transmitted per Hold interval. */
    public static final int BGP_KEEPALIVE_PER_HOLD_INTERVAL = 3;

    /** BGP KEEPALIVE messages minimum Holdtime (in seconds). */
    public static final int BGP_KEEPALIVE_MIN_HOLDTIME = 3;

    /** BGP KEEPALIVE messages minimum transmission interval (in seconds). */
    public static final int BGP_KEEPALIVE_MIN_INTERVAL = 1;

    /** BGP AS 0 (zero) value. See draft-ietf-idr-as0-06.txt Internet Draft. */
    public static final long BGP_AS_0 = 0;

    /**
     * BGP OPEN related constants.
     */
    public static final class Open {
        /**
         * Default constructor.
         * <p>
         * The constructor is private to prevent creating an instance of
         * this utility class.
         */
        private Open() {
        }

        /**
         * BGP OPEN: Optional Parameters related constants.
         */
        public static final class OptionalParameters {
        }

        /**
         * BGP OPEN: Capabilities related constants (RFC 5492).
         */
        public static final class Capabilities {
            /** BGP OPEN Optional Parameter Type: Capabilities. */
            public static final int TYPE = 2;

            /** BGP OPEN Optional Parameter minimum length. */
            public static final int MIN_LENGTH = 2;

            /**
             * BGP OPEN: Multiprotocol Extensions Capabilities (RFC 4760).
             */
            public static final class MultiprotocolExtensions {
                /** BGP OPEN Multiprotocol Extensions code. */
                public static final int CODE = 1;

                /** BGP OPEN Multiprotocol Extensions length. */
                public static final int LENGTH = 4;

                /** BGP OPEN Multiprotocol Extensions AFI: IPv4. */
                public static final int AFI_IPV4 = 1;

                /** BGP OPEN Multiprotocol Extensions AFI: IPv6. */
                public static final int AFI_IPV6 = 2;

                /** BGP OPEN Multiprotocol Extensions SAFI: unicast. */
                public static final int SAFI_UNICAST = 1;

                /** BGP OPEN Multiprotocol Extensions SAFI: multicast. */
                public static final int SAFI_MULTICAST = 2;
            }

            /**
             * BGP OPEN: Support for 4-octet AS Number Capability (RFC 6793).
             */
            public static final class As4Octet {
                /** BGP OPEN Support for 4-octet AS Number Capability code. */
                public static final int CODE = 65;

                /** BGP OPEN 4-octet AS Number Capability length. */
                public static final int LENGTH = 4;
            }
        }
    }

    /**
     * BGP UPDATE related constants.
     */
    public static final class Update {
        /**
         * Default constructor.
         * <p>
         * The constructor is private to prevent creating an instance of
         * this utility class.
         */
        private Update() {
        }

        /** BGP AS length. */
        public static final int AS_LENGTH = 2;

        /** BGP 4 Octet AS length (RFC 6793). */
        public static final int AS_4OCTET_LENGTH = 4;

        /**
         * BGP UPDATE: ORIGIN related constants.
         */
        public static final class Origin {
            /**
             * Default constructor.
             * <p>
             * The constructor is private to prevent creating an instance of
             * this utility class.
             */
            private Origin() {
            }

            /** BGP UPDATE Attributes Type Code ORIGIN. */
            public static final int TYPE = 1;

            /** BGP UPDATE Attributes Type Code ORIGIN length. */
            public static final int LENGTH = 1;

            /** BGP UPDATE ORIGIN: IGP. */
            public static final int IGP = 0;

            /** BGP UPDATE ORIGIN: EGP. */
            public static final int EGP = 1;

            /** BGP UPDATE ORIGIN: INCOMPLETE. */
            public static final int INCOMPLETE = 2;

            /**
             * Gets the BGP UPDATE origin type as a string.
             *
             * @param type the BGP UPDATE origin type
             * @return the BGP UPDATE origin type as a string
             */
            public static String typeToString(int type) {
                String typeString = "UNKNOWN";

                switch (type) {
                case IGP:
                    typeString = "IGP";
                    break;
                case EGP:
                    typeString = "EGP";
                    break;
                case INCOMPLETE:
                    typeString = "INCOMPLETE";
                    break;
                default:
                    break;
                }
                return typeString;
            }
        }

        /**
         * BGP UPDATE: AS_PATH related constants.
         */
        public static final class AsPath {
            /**
             * Default constructor.
             * <p>
             * The constructor is private to prevent creating an instance of
             * this utility class.
             */
            private AsPath() {
            }

            /** BGP UPDATE Attributes Type Code AS_PATH. */
            public static final int TYPE = 2;

            /** BGP UPDATE AS_PATH Type: AS_SET. */
            public static final int AS_SET = 1;

            /** BGP UPDATE AS_PATH Type: AS_SEQUENCE. */
            public static final int AS_SEQUENCE = 2;

            /** BGP UPDATE AS_PATH Type: AS_CONFED_SEQUENCE. */
            public static final int AS_CONFED_SEQUENCE = 3;

            /** BGP UPDATE AS_PATH Type: AS_CONFED_SET. */
            public static final int AS_CONFED_SET = 4;

            /**
             * Gets the BGP AS_PATH type as a string.
             *
             * @param type the BGP AS_PATH type
             * @return the BGP AS_PATH type as a string
             */
            public static String typeToString(int type) {
                String typeString = "UNKNOWN";

                switch (type) {
                case AS_SET:
                    typeString = "AS_SET";
                    break;
                case AS_SEQUENCE:
                    typeString = "AS_SEQUENCE";
                    break;
                case AS_CONFED_SEQUENCE:
                    typeString = "AS_CONFED_SEQUENCE";
                    break;
                case AS_CONFED_SET:
                    typeString = "AS_CONFED_SET";
                    break;
                default:
                    break;
                }
                return typeString;
            }
        }

        /**
         * BGP UPDATE: NEXT_HOP related constants.
         */
        public static final class NextHop {
            /**
             * Default constructor.
             * <p>
             * The constructor is private to prevent creating an instance of
             * this utility class.
             */
            private NextHop() {
            }

            /** BGP UPDATE Attributes Type Code NEXT_HOP. */
            public static final int TYPE = 3;

            /** BGP UPDATE Attributes Type Code NEXT_HOP length. */
            public static final int LENGTH = 4;
        }

        /**
         * BGP UPDATE: MULTI_EXIT_DISC related constants.
         */
        public static final class MultiExitDisc {
            /**
             * Default constructor.
             * <p>
             * The constructor is private to prevent creating an instance of
             * this utility class.
             */
            private MultiExitDisc() {
            }

            /** BGP UPDATE Attributes Type Code MULTI_EXIT_DISC. */
            public static final int TYPE = 4;

            /** BGP UPDATE Attributes Type Code MULTI_EXIT_DISC length. */
            public static final int LENGTH = 4;

            /** BGP UPDATE Attributes lowest MULTI_EXIT_DISC value. */
            public static final int LOWEST_MULTI_EXIT_DISC = 0;
        }

        /**
         * BGP UPDATE: LOCAL_PREF related constants.
         */
        public static final class LocalPref {
            /**
             * Default constructor.
             * <p>
             * The constructor is private to prevent creating an instance of
             * this utility class.
             */
            private LocalPref() {
            }

            /** BGP UPDATE Attributes Type Code LOCAL_PREF. */
            public static final int TYPE = 5;

            /** BGP UPDATE Attributes Type Code LOCAL_PREF length. */
            public static final int LENGTH = 4;
        }

        /**
         * BGP UPDATE: ATOMIC_AGGREGATE related constants.
         */
        public static final class AtomicAggregate {
            /**
             * Default constructor.
             * <p>
             * The constructor is private to prevent creating an instance of
             * this utility class.
             */
            private AtomicAggregate() {
            }

            /** BGP UPDATE Attributes Type Code ATOMIC_AGGREGATE. */
            public static final int TYPE = 6;

            /** BGP UPDATE Attributes Type Code ATOMIC_AGGREGATE length. */
            public static final int LENGTH = 0;
        }

        /**
         * BGP UPDATE: AGGREGATOR related constants.
         */
        public static final class Aggregator {
            /**
             * Default constructor.
             * <p>
             * The constructor is private to prevent creating an instance of
             * this utility class.
             */
            private Aggregator() {
            }

            /** BGP UPDATE Attributes Type Code AGGREGATOR. */
            public static final int TYPE = 7;

            /** BGP UPDATE Attributes Type Code AGGREGATOR length: 2 octet AS. */
            public static final int AS2_LENGTH = 6;

            /** BGP UPDATE Attributes Type Code AGGREGATOR length: 4 octet AS. */
            public static final int AS4_LENGTH = 8;
        }

        /**
         * BGP UPDATE: MP_REACH_NLRI related constants.
         */
        public static final class MpReachNlri {
            /**
             * Default constructor.
             * <p>
             * The constructor is private to prevent creating an instance of
             * this utility class.
             */
            private MpReachNlri() {
            }

            /** BGP UPDATE Attributes Type Code MP_REACH_NLRI. */
            public static final int TYPE = 14;

            /** BGP UPDATE Attributes Type Code MP_REACH_NLRI min length. */
            public static final int MIN_LENGTH = 5;
        }

        /**
         * BGP UPDATE: MP_UNREACH_NLRI related constants.
         */
        public static final class MpUnreachNlri {
            /**
             * Default constructor.
             * <p>
             * The constructor is private to prevent creating an instance of
             * this utility class.
             */
            private MpUnreachNlri() {
            }

            /** BGP UPDATE Attributes Type Code MP_UNREACH_NLRI. */
            public static final int TYPE = 15;

            /** BGP UPDATE Attributes Type Code MP_UNREACH_NLRI min length. */
            public static final int MIN_LENGTH = 3;
        }
    }

    /**
     * BGP NOTIFICATION related constants.
     */
    public static final class Notifications {
        /**
         * Default constructor.
         * <p>
         * The constructor is private to prevent creating an instance of
         * this utility class.
         */
        private Notifications() {
        }

        /**
         * BGP NOTIFICATION: Message Header Error constants.
         */
        public static final class MessageHeaderError {
            /**
             * Default constructor.
             * <p>
             * The constructor is private to prevent creating an instance of
             * this utility class.
             */
            private MessageHeaderError() {
            }

            /** Message Header Error code. */
            public static final int ERROR_CODE = 1;

            /** Message Header Error subcode: Connection Not Synchronized. */
            public static final int CONNECTION_NOT_SYNCHRONIZED = 1;

            /** Message Header Error subcode: Bad Message Length. */
            public static final int BAD_MESSAGE_LENGTH = 2;

            /** Message Header Error subcode: Bad Message Type. */
            public static final int BAD_MESSAGE_TYPE = 3;
        }

        /**
         * BGP NOTIFICATION: OPEN Message Error constants.
         */
        public static final class OpenMessageError {
            /**
             * Default constructor.
             * <p>
             * The constructor is private to prevent creating an instance of
             * this utility class.
             */
            private OpenMessageError() {
            }

            /** OPEN Message Error code. */
            public static final int ERROR_CODE = 2;

            /** OPEN Message Error subcode: Unsupported Version Number. */
            public static final int UNSUPPORTED_VERSION_NUMBER = 1;

            /** OPEN Message Error subcode: Bad PEER AS. */
            public static final int BAD_PEER_AS = 2;

            /** OPEN Message Error subcode: Unacceptable Hold Time. */
            public static final int UNACCEPTABLE_HOLD_TIME = 6;
        }

        /**
         * BGP NOTIFICATION: UPDATE Message Error constants.
         */
        public static final class UpdateMessageError {
            /**
             * Default constructor.
             * <p>
             * The constructor is private to prevent creating an instance of
             * this utility class.
             */
            private UpdateMessageError() {
            }

            /** UPDATE Message Error code. */
            public static final int ERROR_CODE = 3;

            /** UPDATE Message Error subcode: Malformed Attribute List. */
            public static final int MALFORMED_ATTRIBUTE_LIST = 1;

            /** UPDATE Message Error subcode: Unrecognized Well-known Attribute. */
            public static final int UNRECOGNIZED_WELL_KNOWN_ATTRIBUTE = 2;

            /** UPDATE Message Error subcode: Missing Well-known Attribute. */
            public static final int MISSING_WELL_KNOWN_ATTRIBUTE = 3;

           /** UPDATE Message Error subcode: Attribute Flags Error. */
            public static final int ATTRIBUTE_FLAGS_ERROR = 4;

            /** UPDATE Message Error subcode: Attribute Length Error. */
            public static final int ATTRIBUTE_LENGTH_ERROR = 5;

            /** UPDATE Message Error subcode: Invalid ORIGIN Attribute. */
            public static final int INVALID_ORIGIN_ATTRIBUTE = 6;

            /** UPDATE Message Error subcode: Invalid NEXT_HOP Attribute. */
            public static final int INVALID_NEXT_HOP_ATTRIBUTE = 8;

            /** UPDATE Message Error subcode: Optional Attribute Error. Unused. */
            public static final int OPTIONAL_ATTRIBUTE_ERROR = 9;

            /** UPDATE Message Error subcode: Invalid Network Field. */
            public static final int INVALID_NETWORK_FIELD = 10;

            /** UPDATE Message Error subcode: Malformed AS_PATH. */
            public static final int MALFORMED_AS_PATH = 11;
        }

        /**
         * BGP NOTIFICATION: Hold Timer Expired constants.
         */
        public static final class HoldTimerExpired {
            /**
             * Default constructor.
             * <p>
             * The constructor is private to prevent creating an instance of
             * this utility class.
             */
            private HoldTimerExpired() {
            }

            /** Hold Timer Expired code. */
            public static final int ERROR_CODE = 4;
        }

        /** BGP NOTIFICATION message Error subcode: Unspecific. */
        public static final int ERROR_SUBCODE_UNSPECIFIC = 0;
    }
}
