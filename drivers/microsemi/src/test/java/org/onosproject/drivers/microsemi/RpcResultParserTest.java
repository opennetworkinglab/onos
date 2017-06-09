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
package org.onosproject.drivers.microsemi;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

public class RpcResultParserTest {

    private static final String SAMPLE1_XML = "/systemReply-Sample1.xml";
    private static final String SAMPLE2_XML = "/systemReply-Sample2.xml";

    @Test
    public void testSerialNumber1() {
        String serialNumberReply = loadXml(SAMPLE1_XML);
        String serialNumber = RpcResultParser.parseXml(serialNumberReply, "serial-number");
        assertEquals("Eagle Simulator.", serialNumber);
    }

    @Test
    public void testSerialNumber2() {
        String serialNumberReply = loadXml(SAMPLE2_XML);
        String serialNumber = RpcResultParser.parseXml(serialNumberReply, "serial-number");
        assertEquals(null, serialNumber);
    }

    @Test
    public void testOsRelease1() {
        String osReleaseReply = loadXml(SAMPLE1_XML);
        String osRelease = RpcResultParser.parseXml(osReleaseReply, "os-release");
        assertEquals("2.6.33-arm1-MSEA1000--00326-g643be76.x.0.0.212", osRelease);
    }

    @Test
    public void testOsRelease2() {
        String osReleaseReply = loadXml(SAMPLE2_XML);
        String osRelease = RpcResultParser.parseXml(osReleaseReply, "os-release");
        assertEquals(null, osRelease);
    }

    @Test
    public void testLongitude() {
        String longitudeReply = loadXml(SAMPLE1_XML);
        String longitudeStr = RpcResultParser.parseXml(longitudeReply, "longitude");
        assertEquals("-8.4683990", longitudeStr);
    }

    @Test
    public void testLatitude() {
        String latitudeReply = loadXml(SAMPLE1_XML);
        String latitudeStr = RpcResultParser.parseXml(latitudeReply, "latitude");
        assertEquals("51.9036140", latitudeStr);
    }


    private static String loadXml(final String fileName) {

        InputStream inputStream = RpcResultParserTest.class.getResourceAsStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder result = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result.toString();
    }
}