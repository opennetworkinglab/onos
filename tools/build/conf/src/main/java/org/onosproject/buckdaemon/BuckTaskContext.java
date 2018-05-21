/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.buckdaemon;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Context for executing a single Buck task.
 */
public class BuckTaskContext {

    private final String taskName;
    private final ImmutableList<String> input;
    private final List<String> output;

    public static BuckTaskContext createBuckTaskContext(InputStream inputStream) throws IOException {
        ImmutableList<String> lines = slurpInput(inputStream);
        if (lines.size() == 0) {
            return null;
        } else {
            return new BuckTaskContext(lines);
        }
    }

    BuckTaskContext(ImmutableList<String> lines) {
        this.taskName = lines.get(0);
        this.input = lines.subList(1, lines.size());
        this.output = Lists.newArrayList();
    }

    /**
     * Reads all input, line by line, from a stream until an empty line or EOF is encountered.
     *
     * @param stream input stream
     * @return the lines of the input
     * @throws IOException
     */
    private static ImmutableList<String> slurpInput(InputStream stream) throws IOException {
        ImmutableList.Builder<String> lines = ImmutableList.builder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
        while (true) {
            String line = bufferedReader.readLine();
            if (line == null || line.trim().length() == 0) {
                // Empty line or EOF
                break;
            }
            lines.add(line);
        }
        return lines.build();
    }

    /**
     * Returns the symbolic task name.
     *
     * @return symbolic task name
     */
    public String taskName() {
        return taskName;
    }

    /**
     * Returns the input data a list of strings.
     *
     * @return input data
     */
    public List<String> input() {
        return ImmutableList.copyOf(input);
    }

    /**
     * Returns the output data a list of strings.
     *
     * @return output data
     */
    List<String> output() {
        return ImmutableList.copyOf(output);
    }

    /**
     * Adds a line to the output data.
     *
     * @param line line of output data
     */
    public void output(String line) {
        output.add(line);
    }

}
