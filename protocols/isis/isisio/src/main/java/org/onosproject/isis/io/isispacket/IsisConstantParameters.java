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

package org.onosproject.isis.io.isispacket;

/**
 * Represents ISIS constant parameters.
 */
public final class IsisConstantParameters {

    public static final int IRPDISCRIMINATOR = 131;
    public static final int PROOCOLID = 1;
    public static final int VERSION = 1;
    public static final int RESERVED = 0;
    public static final int MAXAREAADDRESS = 3;
    public static final int IDLENGTH = 6;
    public static final int PROTOCOLSUPPORTED = 6;
    public static final int PACKETMINIMUMLENGTH = 27;
    public static final int PDULENGTHPOSITION = 17;
    public static final int PDUHEADERFORREADFROM = 8;

    /**
     * Creates an instance of this class.
     */
    private IsisConstantParameters() {

    }
}
