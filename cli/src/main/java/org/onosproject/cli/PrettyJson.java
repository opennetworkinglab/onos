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
package org.onosproject.cli;

import java.io.IOException;
import org.apache.karaf.shell.commands.Command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Pretty print previous command output JSON.
 */
@Command(scope = "onos", name = "pp",
         description = "Pretty print JSON output from previous command")
public class PrettyJson extends AbstractShellCommand {

    @Override
    protected void execute() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            String json = mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(mapper.readTree(System.in));

            print("%s", json);
        } catch (IOException e) {
            log.error("Failed parsing JSON.", e);
            print("%s", e);
        }
    }

}
