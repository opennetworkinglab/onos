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
package org.onlab.packet;

import com.google.common.io.Resources;

import java.nio.ByteBuffer;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for PPPoED class.
 */
public class PPPoEDTest {

    private static final byte VERSION = (byte) 1;
    private static final byte TYPE = (byte) 1;
    private static final short NO_SESSION = (short) 0;
    private static final short SESSION = (short) 0x02e2;

    private static final String AC_NAME = "pfSense.localdomain";
    private static final long AC_COOKIE = 32098554083868671L;
    private static final String SERVICE_NAME = "*";

    private static final String PADI = "pppoed/padi.bin";
    private static final String PADO = "pppoed/pado.bin";
    private static final String PADR = "pppoed/padr.bin";
    private static final String PADS = "pppoed/pads.bin";
    private static final String PADT = "pppoed/padt.bin";

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(PPPoED.deserializer());
    }

    @Test
    public void testTruncatedPacket() throws Exception {
        byte[] byteHeader = ByteBuffer.allocate(6)
                .put((byte) 0x11) // version/type
                .put((byte) 0x09) // code
                .putShort((short) 0x0000) // transaction id
                .putShort((short) 0x0000) // payload length
                .array();

        PacketTestUtils.testDeserializeTruncated(PPPoED.deserializer(), byteHeader);
    }

    @Test
    public void testDeserializePadi() throws Exception {
        byte[] data = Resources.toByteArray(PPPoEDTest.class.getResource(PADI));
        Ethernet eth = Ethernet.deserializer().deserialize(data, 0, data.length);
        PPPoED pppoed = (PPPoED) eth.getPayload();
        assertPPPoEDHeader(pppoed, PPPoED.PPPOED_CODE_PADI);
        assertEquals(NO_SESSION, pppoed.getSessionId());
        assertEquals(0, pppoed.getTags().size());
    }

    @Test
    public void testDeserializePado() throws Exception {
        byte[] data = Resources.toByteArray(PPPoEDTest.class.getResource(PADO));
        Ethernet eth = Ethernet.deserializer().deserialize(data, 0, data.length);

        PPPoED pppoed = (PPPoED) eth.getPayload();
        assertPPPoEDHeader(pppoed, PPPoED.PPPOED_CODE_PADO);
        assertEquals(NO_SESSION, pppoed.getSessionId());

        assertEquals(3, pppoed.getTags().size());

        PPPoEDTag acName = pppoed.getTag(PPPoEDTag.PPPOED_TAG_AC_NAME);
        assertArrayEquals(AC_NAME.getBytes(), acName.value);

        PPPoEDTag serviceName = pppoed.getTag(PPPoEDTag.PPPOED_TAG_SERVICE_NAME);
        assertArrayEquals(SERVICE_NAME.getBytes(), serviceName.value);

        PPPoEDTag acCookie = pppoed.getTag(PPPoEDTag.PPPOED_TAG_AC_COOKIE);
        assertEquals(AC_COOKIE, ByteBuffer.wrap(acCookie.value).getLong());
    }

    @Test
    public void testDeserializePadr() throws Exception {
        byte[] data = Resources.toByteArray(PPPoEDTest.class.getResource(PADR));
        Ethernet eth = Ethernet.deserializer().deserialize(data, 0, data.length);

        PPPoED pppoed = (PPPoED) eth.getPayload();
        assertPPPoEDHeader(pppoed, PPPoED.PPPOED_CODE_PADR);
        assertEquals(NO_SESSION, pppoed.getSessionId());

        assertEquals(1, pppoed.getTags().size());

        PPPoEDTag acCookie = pppoed.getTag(PPPoEDTag.PPPOED_TAG_AC_COOKIE);
        assertEquals(AC_COOKIE, ByteBuffer.wrap(acCookie.value).getLong());
    }

    @Test
    public void testDeserializePads() throws Exception {
        byte[] data = Resources.toByteArray(PPPoEDTest.class.getResource(PADS));
        Ethernet eth = Ethernet.deserializer().deserialize(data, 0, data.length);

        PPPoED pppoed = (PPPoED) eth.getPayload();
        assertPPPoEDHeader(pppoed, PPPoED.PPPOED_CODE_PADS);
        assertEquals(SESSION, pppoed.getSessionId());

        assertEquals(2, pppoed.getTags().size());
        PPPoEDTag acName = pppoed.getTag(PPPoEDTag.PPPOED_TAG_AC_NAME);
        assertArrayEquals(AC_NAME.getBytes(), acName.value);

        PPPoEDTag acCookie = pppoed.getTag(PPPoEDTag.PPPOED_TAG_AC_COOKIE);
        assertEquals(AC_COOKIE, ByteBuffer.wrap(acCookie.value).getLong());
    }

    @Test
    public void testDeserializePadt() throws Exception {
        byte[] data = Resources.toByteArray(PPPoEDTest.class.getResource(PADT));
        Ethernet eth = Ethernet.deserializer().deserialize(data, 0, data.length);

        PPPoED pppoed = (PPPoED) eth.getPayload();
        assertPPPoEDHeader(pppoed, PPPoED.PPPOED_CODE_PADT);
        assertEquals(SESSION, pppoed.getSessionId());

        assertEquals(1, pppoed.getTags().size());
        PPPoEDTag acCookie = pppoed.getTag(PPPoEDTag.PPPOED_TAG_AC_COOKIE);
        assertEquals(AC_COOKIE, ByteBuffer.wrap(acCookie.value).getLong());
    }

    private void assertPPPoEDHeader(PPPoED pppoed, byte code) {
        assertNotNull(pppoed);
        assertEquals(VERSION, pppoed.getVersion());
        assertEquals(TYPE, pppoed.getType());
        assertEquals(code, pppoed.getCode());
    }

}
