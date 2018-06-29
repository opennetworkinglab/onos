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
package org.onosproject.libgen;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Generates a BUCK file from a JSON file containing third-party library
 * dependencies.
 */
public class BuckLibGenerator {

//    public static final String MAVEN_COORDS = "maven_coords";
//    public static final String COMPILE_ONLY = "compile_only";
//    public static final String RUNTIME_ONLY = "runtime_only";

    private final ObjectNode jsonRoot;
    private final List<BuckArtifact> artifacts = new ArrayList<>();
    private final List<BuckLibrary> libraries = new ArrayList<>();

    private static boolean generateForBazel = false;

    /**
     * Main entry point.
     *
     * @param args command-line arguments; JSON input file and BUCK output file
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Not enough args.\n\nUSAGE: <json file> <output>");
            System.exit(5);
        }

        if (args.length == 3 && "--bazel".equals(args[2])) {
            generateForBazel = true;
        }

        // Parse args
        String jsonFilePath = args[0];
        String outputBuckPath = args[1];

        // Load and parse input JSON file
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        ObjectNode json = (ObjectNode) mapper.reader()
                .readTree(new FileInputStream(jsonFilePath));

        // Traverse dependencies and build a dependency graph (DAG)
        BuckLibGenerator generator = new BuckLibGenerator(json).resolve();

        // Write the output BUCK file
        generator.write(outputBuckPath);
        System.out.printf("\nFinish writing %s\n", outputBuckPath);
    }

    public BuckLibGenerator(ObjectNode root) {
        this.jsonRoot = root;
    }

    private BuckArtifact parseArtifact(Map.Entry<String, JsonNode> entry) {
        String name = entry.getKey();
        JsonNode value = entry.getValue();
        String uri;
        String repo = null;
        if (value.isTextual()) {
            uri = value.asText();
        } else if (value.isObject()) {
            uri = value.get("uri").asText();
            repo = value.get("repo").asText("");
        } else {
            throw new RuntimeException("Unknown element for name: " + name +
                                       " of type: " + value.getNodeType());
        }

        System.out.print(name + " ");
        System.out.flush();
        BuckArtifact buckArtifact;
        if (uri.startsWith("http")) {
            String sha = generateForBazel ? getHttpSha256(name, uri) : getHttpSha1(name, uri);
            buckArtifact = BuckArtifact.getArtifact(name, uri, sha, generateForBazel);
        } else if (uri.startsWith("mvn")) {
            uri = uri.replaceFirst("mvn:", "");
//            if (repo != null) {
//                System.out.println(name + " " + repo);
//            }
            buckArtifact = AetherResolver.getArtifact(name, uri, repo, generateForBazel);
        } else {
            throw new RuntimeException("Unsupported artifact uri: " + uri);
        }
        System.out.println(buckArtifact.url());
        return buckArtifact;
    }

    private BuckLibrary parseLibrary(Map.Entry<String, JsonNode> entry) {
        String libraryName = entry.getKey();
        JsonNode list = entry.getValue();
        if (list.size() == 0) {
            throw new RuntimeException("Empty library: " + libraryName);
        }

        List<String> libraryTargets = new ArrayList<>(list.size());
        list.forEach(node -> {
            String name;
            if (node.isObject()) {
                name = node.get("name").asText();
            } else if (node.isTextual()) {
                name = node.asText();
            } else {
                throw new RuntimeException("Unknown node type: " + node.getNodeType());
            }
            if (!name.contains(":")) {
                name = ':' + name;
            }
            libraryTargets.add(name);
        });

        return BuckLibrary.getLibrary(libraryName, libraryTargets, generateForBazel);
    }

    public BuckLibGenerator resolve() {
        jsonRoot.get("artifacts").fields().forEachRemaining(entry -> {
            BuckArtifact buckArtifact = parseArtifact(entry);
            artifacts.add(buckArtifact);
//            String artifactName = buckArtifact.name();
//            if (artifacts.putIfAbsent(artifactName, buckArtifact) != null) {
//                error("Duplicate artifact: %s", artifactName);
//            }
        });

        jsonRoot.get("libraries").fields().forEachRemaining(entry -> {
            BuckLibrary library = parseLibrary(entry);
            libraries.add(library);
//            String libraryName = library.name();
//            if (libraries.putIfAbsent(libraryName, library) != null) {
//                error("Duplicate library: %s", libraryName);
//            }
        });

        return this;
    }

    private String generateArtifacts() {
        StringBuilder sb = new StringBuilder();
        if (!generateForBazel) {
            artifacts.forEach(artifact -> sb.append(artifact.getBuckFragment()));
        } else {
            StringBuilder mavenJars = new StringBuilder();
            StringBuilder javaLibraries = new StringBuilder();

            mavenJars.append("\ndef generated_maven_jars():");
            javaLibraries.append("\ndef generated_java_libraries():");

            artifacts.forEach(artifact -> {
                mavenJars.append(artifact.getBazelMavenJarFragment());
                javaLibraries.append(artifact.getBazelJavaLibraryFragment());
            });

            sb.append(mavenJars).append(javaLibraries);
        }
        return sb.toString();
    }

    private String generateArtifactMap() {
        StringBuilder artifactMap = new StringBuilder();

        artifactMap.append("\nartifact_map = {}");

        artifacts.forEach(artifact -> {
            artifactMap.append("\nartifact_map[str(Label(\"" + artifact.bazelExport() + "\"))] = \"" + artifact.url(true) + "\"");
        });

        artifactMap.append("\n\ndef maven_coordinates(label):\n" +
                "    label_string = str(label)\n" +
                "    if label_string in artifact_map:\n" +
                "        return artifact_map[label_string]\n" +
                "    else:\n" +
                "        return \"mvn:%s:%s:%s\" % (ONOS_GROUP_ID, label.name, ONOS_VERSION)\n");

        return artifactMap.toString();
    }

    void write(String outputFilePath) {
        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("UTC"));
        File outputFile = new File(outputFilePath);
        if (!outputFile.setWritable(true)) {
            error("Failed to make %s to writeable.", outputFilePath);
        }
        try (PrintWriter writer = new PrintWriter(outputFile)) {
            writer.write(String.format(
                    "# ***** This file was auto-generated at %s. Do not edit this file manually. *****\n",
                    formatter.format(Instant.now())));
            writer.write("# ***** Use onos-lib-gen *****\n");

            if (!generateForBazel) {
                // TODO - have to do this somehow for bazel
                // rule to publish the onos dependency pom
                writer.write("\npass_thru_pom(\n" +
                        "    name = 'onos-dependencies-pom',\n" +
                        "    src = 'pom.xml',\n" +
                        "    out = 'onos-dependencies.pom',\n" +
                        "    artifactId = 'onos-dependencies',\n" +
                        ")\n\n");
            } else {
                writer.write("\nload(\"//tools/build/bazel:variables.bzl\", \"ONOS_GROUP_ID\", \"ONOS_VERSION\")\n\n");
            }

            libraries.forEach(library -> writer.print(library.getFragment()));
            writer.print(generateArtifacts());
            if (generateForBazel) {
                writer.print(generateArtifactMap());
            }
            writer.flush();
        } catch (FileNotFoundException e) {
            error("File not found: %s", outputFilePath);
        }
        if (!outputFile.setReadOnly()) {
            error("Failed to set %s to read-only.", outputFilePath);
        }
    }

    String getHttpSha1(String name, String urlStr) {
        return getHttpSha(name, urlStr, "SHA-1");
    }

    String getHttpSha256(String name, String urlStr) {
        return getHttpSha(name, urlStr, "SHA-256");
    }

    String getHttpSha(String name, String urlStr, String algorithm) {
        //FIXME need http download cache
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[8192];

            URL url = new URL(urlStr);
            String fname = new File(url.getPath()).getName();

            // naively look for cache in buck-out/gen/
            // only works for `buck fetch`-ed artifacts
            // (=doesn't work for bndexe unless manually fetched)
            Optional<File> cache = Optional.ofNullable(System.getenv("ONOS_ROOT"))
                .map(Paths::get)
                .map(Stream::of)
                .orElseGet(Stream::empty)
                // look for remote_file, remote_jar path
                .flatMap(root -> Stream.of(root.resolve("buck-out/gen/lib/" + name + "/" + fname),
                                           root.resolve("buck-out/gen/lib/" + fname + "/" + fname)))
                .map(Path::toFile)
                .filter(File::canRead)
                .findAny();

            if (cache.isPresent() && !generateForBazel) {
                try (FileInputStream stream = new FileInputStream(cache.get())) {
                    int read;
                    while ((read = stream.read(buffer)) >= 0) {
                        md.update(buffer, 0, read);
                    }
                    StringBuilder result = new StringBuilder();
                    byte[] digest = md.digest();
                    for (byte b : digest) {
                        result.append(String.format("%02x", b));
                    }
                    return result.toString();
                } catch (IOException e) {
                    // fall back to regular download
                }
            }
            URLConnection connection = url.openConnection();
            connection.connect();
            InputStream stream = connection.getInputStream();

            int read;
            while ((read = stream.read(buffer)) >= 0) {
                md.update(buffer, 0, read);
            }
            StringBuilder result = new StringBuilder();
            byte[] digest = md.digest();
            for (byte b : digest) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void error(String format, String... args) {
        if (!format.endsWith("\n")) {
            format += '\n';
        }
        System.err.printf(format, args);
        System.exit(1);
    }
}
