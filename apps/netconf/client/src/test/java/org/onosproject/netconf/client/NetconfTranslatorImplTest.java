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

package org.onosproject.netconf.client;

import com.google.common.annotations.Beta;
import org.junit.Test;
import org.onosproject.netconf.client.impl.NetconfTranslatorImpl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link NetconfTranslatorImpl}.
 */
@Beta
public class NetconfTranslatorImplTest {
    private static final String CORE_GET_CONFIG_MESSAGE_REGEX =
            "<data>\n?\\s*(.*?)\n?\\s*</data>";
    private static final int GET_CONFIG_CORE_MESSAGE_GROUP = 1;
    private static final Pattern GET_CONFIG_CORE_MESSAGE_PATTERN =
            Pattern.compile(CORE_GET_CONFIG_MESSAGE_REGEX, Pattern.DOTALL);
    private static final String GET_CORE_MESSAGE_REGEX = "<data>\n?\\s*(.*?)\n?\\s*</data>";
    private static final int GET_CORE_MESSAGE_GROUP = 1;
    private static final Pattern GET_CORE_MESSAGE_PATTERN =
            Pattern.compile(GET_CORE_MESSAGE_REGEX, Pattern.DOTALL);

    private static final String SAMPLE_GET_REPLY = "<rpc-reply message-id=\"101\"\n" +
            "       xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
            "    <data>\n" +
            "      <t:top xmlns:t=\"http://example.com/schema/1.2/stats\">\n" +
            "        <t:interfaces>\n" +
            "          <t:interface t:ifName=\"eth0\">\n" +
            "            <t:ifInOctets>45621</t:ifInOctets>\n" +
            "            <t:ifOutOctets>774344</t:ifOutOctets>\n" +
            "          </t:interface>\n" +
            "        </t:interfaces>\n" +
            "      </t:top>\n" +
            "    </data>\n" +
            "  </rpc-reply>";
    private static final String CORRECT_FILTERED_GET_REPLY =
            "<t:top xmlns:t=\"http://example.com/schema/1.2/stats\">\n" +
            "        <t:interfaces>\n" +
            "          <t:interface t:ifName=\"eth0\">\n" +
            "            <t:ifInOctets>45621</t:ifInOctets>\n" +
            "            <t:ifOutOctets>774344</t:ifOutOctets>\n" +
            "          </t:interface>\n" +
            "        </t:interfaces>\n" +
            "      </t:top>";
    private static final String SAMPLE_GET_CONFIG_REPLY = "<rpc-reply message-id=\"101\"\n" +
            "          xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
            "       <data>\n" +
            "         <top xmlns=\"http://example.com/schema/1.2/config\">\n" +
            "           <users>\n" +
            "             <user>\n" +
            "               <name>root</name>\n" +
            "               <type>superuser</type>\n" +
            "               <full-name>Charlie Root</full-name>         <company-info>\n" +
            "                 <dept>1</dept>\n" +
            "                 <id>1</id>\n" +
            "               </company-info>\n" +
            "             </user>\n" +
            "             <!-- additional <user> elements appear here... -->\n" +
            "           </users>\n" +
            "         </top>\n" +
            "       </data>\n" +
            "     </rpc-reply>";
    private static final String CORRECT_FILTERED_GET_CONFIG_REPLY =
            "<top xmlns=\"http://example.com/schema/1.2/config\">\n" +
            "           <users>\n" +
            "             <user>\n" +
            "               <name>root</name>\n" +
            "               <type>superuser</type>\n" +
            "               <full-name>Charlie Root</full-name>         <company-info>\n" +
            "                 <dept>1</dept>\n" +
            "                 <id>1</id>\n" +
            "               </company-info>\n" +
            "             </user>\n" +
            "             <!-- additional <user> elements appear here... -->\n" +
            "           </users>\n" +
            "         </top>";


    @Test
    public void testRegex() {
        //Basic check for the getConfig regex.
        Matcher matcher = GET_CONFIG_CORE_MESSAGE_PATTERN.matcher(SAMPLE_GET_CONFIG_REPLY);
        matcher.find();
//        System.out.println(matcher.group(1));
//        System.out.println(DESIRED_SUBSTRING_GET_CONFIG);
        assertEquals("Messages did not match",
                    CORRECT_FILTERED_GET_CONFIG_REPLY,
                    matcher.group(GET_CONFIG_CORE_MESSAGE_GROUP));
        //Basic check for the get regex.
        matcher = GET_CORE_MESSAGE_PATTERN.matcher(SAMPLE_GET_REPLY);
        matcher.find();
//        System.out.println(matcher.group(1));
//        System.out.println(DESIRED_SUBSTRING_GET_CONFIG);
        assertEquals("Messages did not match", CORRECT_FILTERED_GET_REPLY, matcher.group(GET_CORE_MESSAGE_GROUP));
    }
}
