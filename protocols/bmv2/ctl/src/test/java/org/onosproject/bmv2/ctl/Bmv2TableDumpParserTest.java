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

package org.onosproject.bmv2.ctl;

import org.junit.Test;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.runtime.Bmv2ParsedTableEntry;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class Bmv2TableDumpParserTest {

    private Bmv2Configuration configuration = Bmv2DeviceContextServiceImpl.loadDefaultConfiguration();

    @Test
    public void testParse() throws Exception {
        String text = readFile();
        List<Bmv2ParsedTableEntry> result = Bmv2TableDumpParser.parse(text, configuration);
        assertThat(result.size(), equalTo(10));
    }

    private String readFile()
            throws IOException, URISyntaxException {
        byte[] encoded = Files.readAllBytes(Paths.get(this.getClass().getResource("/tabledump.txt").toURI()));
        return new String(encoded, Charset.defaultCharset());
    }
}
