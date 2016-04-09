/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.ospf.protocol.util;

/**
 * Representation of an OSPF configuration parameters and constants.
 */
public final class OspfParameters {

    public static final int LSREFRESHTIME = 1800; //max time between updates;
    public static final int MINLSINTERVAL = 5; // set to 5 second
    public static final int MINLSARRIVAL = 1; // set to 1 second
    public static final int MAXAGE = 3600; // set to 1 hour in seconds
    public static final int CHECKAGE = 300; // set to 5 mins
    public static final int MAXAGEDIFF = 900; // set to 15 mins
    public static final long MAXSEQUENCENUMBER = 2147483647;
    public static final long STARTLSSEQUENCENUM = -2147483647;
    public static final int AGECOUNTER = 1;
    public static final String VERIFYCHECKSUM = "verifyChecksum";
    public static final String REFRESHLSA = "refreshLsa";
    public static final String MAXAGELSA = "maxAgeLsa";
    public static final int START_NOW = 0;
    public static final int TRAFFIC_ENGINEERING = 1;
    public static final int INITIAL_BANDWIDTH = 12500000;
    public static final int ROUTER = 1;
    public static final int NETWORK = 2;
    public static final int SUMMARY = 3;
    public static final int ASBR_SUMMARY = 4;
    public static final int EXTERNAL_LSA = 5;
    public static final int LINK_LOCAL_OPAQUE_LSA = 9;
    public static final int AREA_LOCAL_OPAQUE_LSA = 10;
    public static final int AS_OPAQUE_LSA = 11;
    public static final int HELLO = 1;
    public static final int DD = 2;
    public static final int LSREQUEST = 3;
    public static final int LSUPDATE = 4;
    public static final int LSACK = 5;
    public static final int INFTRA_NS_DELAY = 1;
    public static final int BDR = 6;
    public static final int DR = 7;
    public static final String OPAQUE_ENABLED_OPTION_VALUE = "01000010";

    private OspfParameters() {
    }
}