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
package org.onosproject.lisp.msg.types.lcaf;

import com.google.common.testing.EqualsTester;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.lcaf.LispGeoCoordinateLcafAddress.GeoCoordinateAddressBuilder;
import org.onosproject.lisp.msg.types.lcaf.LispGeoCoordinateLcafAddress.GeoCoordinateLcafAddressReader;
import org.onosproject.lisp.msg.types.lcaf.LispGeoCoordinateLcafAddress.GeoCoordinateLcafAddressWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispGeoCoordinateLcafAddress class.
 */
public class LispGeoCoordinateLcafAddressTest {

    private static final String IP_ADDRESS_1 = "192.168.1.1";
    private static final String IP_ADDRESS_2 = "192.168.1.2";

    private LispGeoCoordinateLcafAddress address1;
    private LispGeoCoordinateLcafAddress sameAsAddress1;
    private LispGeoCoordinateLcafAddress address2;

    @Before
    public void setup() {

        GeoCoordinateAddressBuilder builder1 = new GeoCoordinateAddressBuilder();

        LispIpv4Address ipv4Address1 =
                        new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_1));

        address1 = builder1
                        .withIsNorth(true)
                        .withLatitudeDegree((short) 1)
                        .withLatitudeMinute((byte) 1)
                        .withLatitudeSecond((byte) 1)
                        .withIsEast(false)
                        .withLongitudeDegree((short) 1)
                        .withLongitudeMinute((byte) 1)
                        .withLongitudeSecond((byte) 1)
                        .withAltitude(1)
                        .withAddress(ipv4Address1)
                        .build();

        GeoCoordinateAddressBuilder builder2 = new GeoCoordinateAddressBuilder();

        sameAsAddress1 = builder2
                            .withIsNorth(true)
                            .withLatitudeDegree((short) 1)
                            .withLatitudeMinute((byte) 1)
                            .withLatitudeSecond((byte) 1)
                            .withIsEast(false)
                            .withLongitudeDegree((short) 1)
                            .withLongitudeMinute((byte) 1)
                            .withLongitudeSecond((byte) 1)
                            .withAltitude(1)
                            .withAddress(ipv4Address1)
                            .build();

        GeoCoordinateAddressBuilder builder3 = new GeoCoordinateAddressBuilder();

        LispIpv4Address ipv4Address2 =
                        new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_2));

        address2 = builder3
                        .withIsNorth(false)
                        .withLatitudeDegree((short) 2)
                        .withLatitudeMinute((byte) 2)
                        .withLatitudeSecond((byte) 2)
                        .withIsEast(true)
                        .withLongitudeDegree((short) 2)
                        .withLongitudeMinute((byte) 2)
                        .withLongitudeSecond((byte) 2)
                        .withAltitude(2)
                        .withAddress(ipv4Address2)
                        .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(address1, sameAsAddress1)
                .addEqualityGroup(address2).testEquals();
    }

    @Test
    public void testConstruction() {
        LispGeoCoordinateLcafAddress address = address1;

        LispIpv4Address ipv4Address =
                        new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_1));

        assertThat(address.isNorth(), is(true));
        assertThat(address.getLatitudeDegree(), is((short) 1));
        assertThat(address.getLatitudeMinute(), is((byte) 1));
        assertThat(address.getLatitudeSecond(), is((byte) 1));
        assertThat(address.isEast(), is(false));
        assertThat(address.getLongitudeDegree(), is((short) 1));
        assertThat(address.getLongitudeMinute(), is((byte) 1));
        assertThat(address.getLongitudeSecond(), is((byte) 1));
        assertThat(address.getAltitude(), is(1));
        assertThat(address.getAddress(), is(ipv4Address));
    }

    @Test
    public void testSerialization() throws LispWriterException,
                                           LispParseError, LispReaderException {

        ByteBuf byteBuf = Unpooled.buffer();

        GeoCoordinateLcafAddressWriter writer = new GeoCoordinateLcafAddressWriter();
        writer.writeTo(byteBuf, address1);

        GeoCoordinateLcafAddressReader reader = new GeoCoordinateLcafAddressReader();
        LispGeoCoordinateLcafAddress deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(address1, deserialized).testEquals();
    }
}
