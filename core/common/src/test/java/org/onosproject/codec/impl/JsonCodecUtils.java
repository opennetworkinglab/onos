/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.codec.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.onosproject.net.DeviceId.deviceId;

import org.onlab.packet.ChassisId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.provider.ProviderId;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JsonCodec test utilities.
 */
public abstract class JsonCodecUtils {

    /**
     * Checks if given Object can be encoded to JSON and back.
     *
     * @param context CodecContext
     * @param codec JsonCodec
     * @param pojoIn Java Object to encode.
     *               Object is expected to have #equals implemented.
     */
    public static <T> void assertJsonEncodable(final CodecContext context,
                            final JsonCodec<T> codec,
                            final T pojoIn) {
        final ObjectNode json = codec.encode(pojoIn, context);

        assertThat(json, is(notNullValue()));

        final T pojoOut = codec.decode(json, context);
        assertThat(pojoOut, is(notNullValue()));

        assertEquals(pojoIn, pojoOut);
    }

    static final ProviderId PID = new ProviderId("of", "foo");
    static final ProviderId PIDA = new ProviderId("of", "bar", true);
    static final DeviceId DID1 = deviceId("of:foo");
    static final DeviceId DID2 = deviceId("of:bar");
    static final String MFR = "whitebox";
    static final String HW = "1.1.x";
    static final String SW1 = "3.8.1";
    static final String SW2 = "3.9.5";
    static final String SN = "43311-12345";
    static final ChassisId CID = new ChassisId();
    static final PortNumber P1 = PortNumber.portNumber(1);
    static final PortNumber P2 = PortNumber.portNumber(2);
    static final PortNumber P3 = PortNumber.portNumber(3);
    static final SparseAnnotations A1 = DefaultAnnotations.builder()
                                        .set("A1", "a1")
                                        .set("B1", "b1")
                                        .build();
    static final ConnectPoint CP1 = new ConnectPoint(DID1, P1);
    static final ConnectPoint CP2 = new ConnectPoint(DID2, P2);

}
