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
package org.onlab.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class XmlStringTest {

    @Test
    public void testToString() {
        String input = "<root>   <a some='foo '/><b attr='4' ><c>42</c></b></root>";
        CharSequence xml = XmlString.prettifyXml(input);
        String expected = "<root>\n"
                        + "  <a some=\"foo \"/>\n"
                        + "  <b attr=\"4\">\n"
                        + "    <c>42</c>\n"
                        + "  </b>\n"
                        + "</root>\n";
        assertEquals(expected, xml.toString());
    }

    @Test
    public void illFormed() {
        String input = "<root>   <a some='foo '/>";

        assertEquals(input, XmlString.prettifyXml(input).toString());
    }

    @Test
    public void fragments() {
        String input = "<root/>   <a some='foo '/>";
        String expected = "<root/>\n"
                        + "<a some=\"foo \"/>\n";
        assertEquals(expected, XmlString.prettifyXml(input).toString());
    }

}
