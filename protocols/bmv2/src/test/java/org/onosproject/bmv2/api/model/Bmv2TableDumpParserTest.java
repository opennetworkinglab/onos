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

package org.onosproject.bmv2.api.model;

import org.junit.Test;
import org.onosproject.bmv2.ctl.Bmv2TableDumpParser;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class Bmv2TableDumpParserTest {

    @Test
    public void testParse() throws Exception, Bmv2TableDumpParser.Bmv2TableDumpParserException {

        String text =
                "0: 0000 000000000000 000000000000 &&& 0000000000000000000000000000 => send_to_cpu -\n" +
                        "1: 0000 000000000000 000000000000 &&& 0000000000000000000000000000 => send_to_cpu -\n" +
                        "2: 0000 000000000000 000000000000 &&& 0000000000000000000000000000 => send_to_cpu -\n" +
                        "3: 0000 000000000000 000000000000 &&& 0000000000000000000000000000 => send_to_cpu -";

        Bmv2TableDumpParser parser = new Bmv2TableDumpParser();

        List<Long> result = parser.getEntryIds(text);

        assertThat("invalid parsed values", result.get(0), is(equalTo(0L)));
        assertThat("invalid parsed values", result.get(1), is(equalTo(1L)));
        assertThat("invalid parsed values", result.get(2), is(equalTo(2L)));
        assertThat("invalid parsed values", result.get(3), is(equalTo(3L)));
    }
}
