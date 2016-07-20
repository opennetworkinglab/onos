/*
 * Copyright 2014-present Open Networking Laboratory
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



package org.onlab.packet;

public enum DHCPPacketType {
    // From RFC 1533
    DHCPDISCOVER(1), DHCPOFFER(2), DHCPREQUEST(3), DHCPDECLINE(4), DHCPACK(5), DHCPNAK(
            6), DHCPRELEASE(7),

    // From RFC2132
    DHCPINFORM(8),

    // From RFC3203
    DHCPFORCERENEW(9),

    // From RFC4388
    DHCPLEASEQUERY(10), DHCPLEASEUNASSIGNED(11), DHCPLEASEUNKNOWN(12), DHCPLEASEACTIVE(
            13);

    protected int value;

    private DHCPPacketType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        switch (this.value) {
        case 1:
            return "DHCPDISCOVER";
        case 2:
            return "DHCPOFFER";
        case 3:
            return "DHCPREQUEST";
        case 4:
            return "DHCPDECLINE";
        case 5:
            return "DHCPACK";
        case 6:
            return "DHCPNAK";
        case 7:
            return "DHCPRELEASE";
        case 8:
            return "DHCPINFORM";
        case 9:
            return "DHCPFORCERENEW";
        case 10:
            return "DHCPLEASEQUERY";
        case 11:
            return "DHCPLEASEUNASSIGNED";
        case 12:
            return "DHCPLEASEUNKNOWN";
        case 13:
            return "DHCPLEASEACTIVE";
        default:
            break;
        }

        return null;
    }

    public static DHCPPacketType getType(final int value) {
        switch (value) {
        case 1:
            return DHCPDISCOVER;
        case 2:
            return DHCPOFFER;
        case 3:
            return DHCPREQUEST;
        case 4:
            return DHCPDECLINE;
        case 5:
            return DHCPACK;
        case 6:
            return DHCPNAK;
        case 7:
            return DHCPRELEASE;
        case 8:
            return DHCPINFORM;
        case 9:
            return DHCPFORCERENEW;
        case 10:
            return DHCPLEASEQUERY;
        case 11:
            return DHCPLEASEUNASSIGNED;
        case 12:
            return DHCPLEASEUNKNOWN;
        case 13:
            return DHCPLEASEACTIVE;
        default:
            break;
        }

        return null;
    }
}
