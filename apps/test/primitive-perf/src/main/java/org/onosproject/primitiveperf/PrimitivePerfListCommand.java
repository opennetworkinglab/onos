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
package org.onosproject.primitiveperf;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.primitiveperf.PrimitivePerfCollector.Sample;

/**
 * Displays accumulated performance metrics.
 */
@Service
@Command(scope = "onos", name = "primitive-perf",
        description = "Displays accumulated performance metrics")
public class PrimitivePerfListCommand extends AbstractShellCommand {

    @Option(name = "-s", aliases = "--summary", description = "Output just summary",
            required = false, multiValued = false)
    private boolean summary = false;

    @Override
    protected void doExecute() {
        if (summary) {
            printSummary();
        } else {
            printSamples();
        }
    }

    private void printSummary() {
        PrimitivePerfCollector collector = get(PrimitivePerfCollector.class);
        List<String> headers = collector.getSampleHeaders();
        Sample overall = collector.getOverall();
        double total = 0;
        print("%12s: %14s", "Node ID", "Overall Rate");
        for (int i = 0; i < overall.data.length; i++) {
            if (overall.data[i] >= 0) {
                print("%12s: %14.2f", headers.get(i), overall.data[i]);
                total += overall.data[i];
            } else {
                print("%12s: %14s", headers.get(i), " ");
            }
        }
        print("%12s: %14.2f", "total", total);
    }

    private void printSamples() {
        PrimitivePerfCollector collector = get(PrimitivePerfCollector.class);
        List<String> headers = collector.getSampleHeaders();
        List<Sample> samples = collector.getSamples();

        System.out.print(String.format("%10s  ", "Time"));
        for (String header : headers) {
            System.out.print(String.format("%12s  ", header));
        }
        System.out.println(String.format("%12s", "Total"));

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        for (Sample sample : samples) {
            double total = 0;
            System.out.print(String.format("%10s  ", sdf.format(new Date(sample.time))));
            for (int i = 0; i < sample.data.length; i++) {
                if (sample.data[i] >= 0) {
                    System.out.print(String.format("%12.2f  ", sample.data[i]));
                    total += sample.data[i];
                } else {
                    System.out.print(String.format("%12s  ", " "));
                }
            }
            System.out.println(String.format("%12.2f", total));
        }
    }

}
