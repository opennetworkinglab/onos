/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.cfg.impl;

import com.google.common.collect.ImmutableSet;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cfg.ConfigProperty.Type;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;

import static org.onosproject.cfg.ConfigProperty.defineProperty;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility for writing and reading configuration property definition file.
 */
public final class ConfigPropertyDefinitions {

    private static final String FMT = "%s|%s|%s|%s\n";
    private static final String SEP = "\\|";
    private static final String COMMENT = "#";

    private static final Logger log = getLogger(ConfigPropertyDefinitions.class);

    private ConfigPropertyDefinitions() {
    }

    /**
     * Writes the specified set of property definitions into the given output
     * stream.
     *
     * @param stream output stream
     * @param props  properties whose definitions are to be written
     * @throws java.io.IOException if unable to write the stream
     */
    public static void write(OutputStream stream, Set<ConfigProperty> props) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(stream))) {
            props.forEach(p -> pw.format(FMT, p.name(), p.type(), p.description(), p.defaultValue()));
        }
    }

    /**
     * Reads the specified input stream and creates from its contents a
     * set of property definitions.
     *
     * @param stream input stream
     * @return properties whose definitions are contained in the stream
     * @throws java.io.IOException if unable to read the stream
     */
    public static Set<ConfigProperty> read(InputStream stream) throws IOException {
        ImmutableSet.Builder<ConfigProperty> builder = ImmutableSet.builder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith(COMMENT)) {
                    String[] f = line.split(SEP, 4);
                    if (f.length < 4) {
                        log.warn("Cannot parse property from line: '{}'. " +
                                "This property will be ignored", line);
                        continue;
                    }
                    builder.add(defineProperty(f[0], Type.valueOf(f[1]), f[2], f[3]));
                }
            }
        }
        return builder.build();
    }

}
