/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onlab.jdvue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

/**
 * Generator of a self-contained HTML file which serves as a GUI for
 * visualizing Java package dependencies carried in the supplied catalog.
 *
 * The HTML file is an adaptation of D3.js Hierarchical Edge Bundling as
 * shown at http://bl.ocks.org/mbostock/7607999.
 *
 * @author Thomas Vachuska
 */
public class DependencyViewer {

    private static final String JPD_EXT = ".db";
    private static final String HTML_EXT = ".html";

    private static final String INDEX = "index.html";
    private static final String D3JS = "d3.v3.min.js";

    private static final String TITLE_PLACEHOLDER = "TITLE_PLACEHOLDER";
    private static final String D3JS_PLACEHOLDER = "D3JS_PLACEHOLDER";
    private static final String DATA_PLACEHOLDER = "DATA_PLACEHOLDER";

    private final Catalog catalog;

    /**
     * Creates a Java package dependency viewer.
     *
     * @param catalog dependency catalog
     */
    public DependencyViewer(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Main program entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Catalog cat = new Catalog();
        DependencyViewer viewer = new DependencyViewer(cat);
        try {
            String path = args[0];
            cat.load(path + JPD_EXT);
            cat.analyze();

            System.err.println(cat);
            viewer.dumpLongestCycle(cat);
            viewer.writeHTMLFile(path);
        } catch (IOException e) {
            System.err.println("Unable to process catalog: " + e.getMessage());
        }
    }

    /**
     * Prints out the longest cycle; just for kicks.
     * @param cat catalog
     */
    private void dumpLongestCycle(Catalog cat) {
        DependencyCycle longest = null;
        for (DependencyCycle cycle : cat.getCycles()) {
            if (longest == null || longest.getCycleSegments().size() < cycle.getCycleSegments().size()) {
                longest = cycle;
            }
        }

        if (longest != null) {
            for (Dependency dependency : longest.getCycleSegments()) {
                System.out.println(dependency);
            }
        }
    }

    /**
     * Writes the HTML catalog file for the given viewer.
     *
     * @param path base file path
     * @throws IOException if issues encountered writing the HTML file
     */
    public void writeHTMLFile(String path) throws IOException {
        String index = slurp(getClass().getResourceAsStream(INDEX));
        String d3js = slurp(getClass().getResourceAsStream(D3JS));

        FileWriter fw = new FileWriter(path + HTML_EXT);
        ObjectWriter writer = new ObjectMapper().writer(); // .writerWithDefaultPrettyPrinter();
        fw.write(index.replace(TITLE_PLACEHOLDER, path)
                         .replace(D3JS_PLACEHOLDER, d3js)
                         .replace(DATA_PLACEHOLDER, writer.writeValueAsString(toJson())));
        fw.close();
    }

    /**
     * Slurps the specified input stream into a string.
     *
     * @param stream input stream to be read
     * @return string containing the contents of the input stream
     * @throws IOException if issues encountered reading from the stream
     */
     static String slurp(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append(System.lineSeparator());
        }
        br.close();
        return sb.toString();
    }

    // Produces a JSON structure designed to drive the hierarchical visual
    // representation of Java package dependencies and any dependency cycles
     private JsonNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("packages", jsonPackages(mapper));
        root.put("cycleSegments", jsonCycleSegments(mapper, catalog.getCycleSegments()));
        root.put("summary", jsonSummary(mapper));
        return root;
    }

    // Produces a JSON summary of dependencies
    private JsonNode jsonSummary(ObjectMapper mapper) {
        ObjectNode summary = mapper.createObjectNode();
        summary.put("packages", catalog.getPackages().size());
        summary.put("sources", catalog.getSources().size());
        summary.put("cycles", catalog.getCycles().size());
        summary.put("cycleSegments", catalog.getCycleSegments().size());
        return summary;
    }

    // Produces a JSON structure with package dependency data
    private JsonNode jsonPackages(ObjectMapper mapper) {
        ArrayNode packages = mapper.createArrayNode();
        for (JavaPackage javaPackage : catalog.getPackages()) {
            packages.add(json(mapper, javaPackage));
        }
        return packages;
    }

    // Produces a JSON structure with all cyclic segments
    private JsonNode jsonCycleSegments(ObjectMapper mapper,
                                       Set<Dependency> segments) {
        ObjectNode cyclicSegments = mapper.createObjectNode();
        for (Dependency dependency : segments) {
            String s = dependency.getSource().name();
            String t = dependency.getTarget().name();
            cyclicSegments.put(t + "-" + s,
                               mapper.createObjectNode().put("s", s).put("t", t));
        }
        return cyclicSegments;
    }

    // Produces a JSON object structure describing the specified Java package.
    private JsonNode json(ObjectMapper mapper, JavaPackage javaPackage) {
        ObjectNode node = mapper.createObjectNode();

        ArrayNode imports = mapper.createArrayNode();
        for (JavaPackage dependency : javaPackage.getDependencies()) {
            imports.add(dependency.name());
        }

        Set<DependencyCycle> packageCycles = catalog.getPackageCycles(javaPackage);
        Set<Dependency> packageCycleSegments = catalog.getPackageCycleSegments(javaPackage);

        node.put("name", javaPackage.name());
        node.put("size", javaPackage.getSources().size());
        node.put("imports", imports);
        node.put("cycleSegments", jsonCycleSegments(mapper, packageCycleSegments));
        node.put("cycleCount", packageCycles.size());
        node.put("cycleSegmentCount", packageCycleSegments.size());
        return node;
    }

}
