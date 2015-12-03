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
package org.onosproject.bgpio.util;

/**
 * Provides Constants usage for BGP.
 */
public final class Constants {
    private Constants() {
    }

    public static final short TYPE_AND_LEN = 4;
    public static final short TYPE_AND_LEN_AS_SHORT = 4;
    public static final short TYPE_AND_LEN_AS_BYTE = 3;
    public static final int ISIS_LEVELONE = 1;
    public static final int ISIS_LEVELTWO = 2;
    public static final int OSPFV2 = 3;
    public static final int DIRECT = 4;
    public static final int STATIC_CONFIGURATION = 5;
    public static final int OSPFV3 = 6;
    public static final short AFI_VALUE = 16388;
    public static final byte VPN_SAFI_VALUE = (byte) 0x80;
    public static final byte SAFI_VALUE = 71;
    public static final int EXTRA_TRAFFIC = 0x01;
    public static final int UNPROTECTED = 0x02;
    public static final int SHARED = 0x04;
    public static final int DEDICATED_ONE_ISTO_ONE = 0x08;
    public static final int DEDICATED_ONE_PLUS_ONE = 0x10;
    public static final int ENHANCED = 0x20;
    public static final int RESERVED = 0x40;
}