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
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Generates a worspace inclusion file from a JSON file containing third-party
 * library dependencies.
 */
public class BazelLibGenerator {

    private final ObjectNode jsonRoot;
    private final List<BazelArtifact> artifacts = new ArrayList<>();
    private final List<BazelLibrary> libraries = new ArrayList<>();

    /**
     * Main entry point.
     *
     * @param args command-line arguments; JSON input file and Bazel workspace output file
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: onos-lib-gen <input-deps.json> <output-workspace.bzl>");
            System.exit(5);
        }

        // Parse args
        String jsonFilePath = args[0];
        String outputWorkspaceFilePath = args[1];

        // Load and parse input JSON file
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        ObjectNode json = (ObjectNode) mapper.reader()
                .readTree(new FileInputStream(jsonFilePath));

        // Traverse dependencies and build a dependency graph (DAG)
        org.onosproject.libgen.BazelLibGenerator generator = new org.onosproject.libgen.BazelLibGenerator(json).resolve();

        // Write the output workspace file
        generator.write(outputWorkspaceFilePath);
        System.out.printf("\nFinish writing %s\n", outputWorkspaceFilePath);
    }

    public BazelLibGenerator(ObjectNode root) {
        this.jsonRoot = root;
    }

    private BazelArtifact parseArtifact(Map.Entry<String, JsonNode> entry) {
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
        BazelArtifact bazelArtifact;
        if (uri.startsWith("http")) {
            String sha = getHttpSha256(name, uri);
            bazelArtifact = BazelArtifact.getArtifact(name, uri, sha);
        } else if (uri.startsWith("mvn")) {
            uri = uri.replaceFirst("mvn:", "");
            bazelArtifact = AetherResolver.getArtifact(name, uri, repo);
        } else {
            throw new RuntimeException("Unsupported artifact uri: " + uri);
        }
        System.out.println(bazelArtifact.url());
        return bazelArtifact;
    }

    private BazelLibrary parseLibrary(Map.Entry<String, JsonNode> entry) {
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

        return BazelLibrary.getLibrary(libraryName, libraryTargets);
    }

    public org.onosproject.libgen.BazelLibGenerator resolve() {
        jsonRoot.get("artifacts").fields().forEachRemaining(entry -> {
            BazelArtifact bazelArtifact = parseArtifact(entry);
            artifacts.add(bazelArtifact);
        });

        jsonRoot.get("libraries").fields().forEachRemaining(entry -> {
            BazelLibrary library = parseLibrary(entry);
            libraries.add(library);
        });

        return this;
    }

    private String generateArtifacts() {
        StringBuilder sb = new StringBuilder();
        StringBuilder mavenJars = new StringBuilder();
        mavenJars.append("\ndef generated_maven_jars():");
        artifacts.forEach(artifact -> {
            mavenJars.append(artifact.getMavenJarFragment());
        });
        sb.append(mavenJars);
        return sb.toString();
    }

    private String generateArtifactMap() {
        StringBuilder artifactMap = new StringBuilder();

        artifactMap.append("\nartifact_map = {}");

        artifacts.forEach(artifact -> {
            artifactMap.append("\nartifact_map[\"" + artifact.bazelExport() + "\"] = \"" + artifact.url(true) + "\"");
        });

        artifactMap.append(
                "\n\n" +
                        "def maven_coordinates(label):\n" +
                        "    label_string = str(label)\n" +
                        "    if label_string in artifact_map:\n" +
                        "        return artifact_map[label_string]\n" +
                        "    if (label_string.endswith(\":jar\")):\n" +
                        "        label_string = label_string.replace(\":jar\", \"\")\n" +
                        "        if label_string in artifact_map:\n" +
                        "            return artifact_map[label_string]\n" +
                        "    if type(label) == \"string\":\n" +
                        "        return \"mvn:%s:%s:%s\" % (ONOS_GROUP_ID, label_string, ONOS_VERSION)\n" +
                        "    return \"mvn:%s:%s:%s\" % (ONOS_GROUP_ID, label.name, ONOS_VERSION)\n"
        );

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

            writer.write("\nload(\"//tools/build/bazel:variables.bzl\", \"ONOS_GROUP_ID\", \"ONOS_VERSION\")\n\n");
            writer.write("\nload(\"@bazel_tools//tools/build_defs/repo:java.bzl\", \"java_import_external\")\n\n");

            libraries.forEach(library -> writer.print(library.getFragment()));
            writer.print(generateArtifacts());
            writer.print(generateArtifactMap());
            writer.flush();
        } catch (FileNotFoundException e) {
            error("File not found: %s", outputFilePath);
        }
        if (!outputFile.setReadOnly()) {
            error("Failed to set %s to read-only.", outputFilePath);
        }
    }

    static String getHttpSha1(String name, String urlStr) {
        return getHttpSha(name, urlStr, "SHA-1");
    }

    static String getHttpSha256(String name, String urlStr) {
        return getHttpSha(name, urlStr, "SHA-256");
    }

    private static String getHttpSha(String name, String urlStr, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[8192];

            URL url = new URL(urlStr);

            Optional<File> cache = Optional.ofNullable(System.getenv("ONOS_ROOT"))
                    .map(Paths::get)
                    .map(Stream::of)
                    .orElseGet(Stream::empty)
                    .map(Path::toFile)
                    .filter(File::canRead)
                    .findAny();

            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

            URLConnection connection;
            String env_http_proxy = System.getenv("HTTP_PROXY");
            if (env_http_proxy != null) {
                List<String> proxyHostInfo = getProxyHostInfo(env_http_proxy);
                Proxy http_proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHostInfo.get(0),
                                                                                    Integer.valueOf(proxyHostInfo.get(1))));

                if ((proxyHostInfo.get(2) != null) && (proxyHostInfo.get(3) != null)) {
                    Authenticator authenticator = new Authenticator() {
                        public PasswordAuthentication getPasswordAuthentication() {
                            return (new PasswordAuthentication(proxyHostInfo.get(2), proxyHostInfo.get(3).toCharArray()));
                        }
                    };

                    Authenticator.setDefault(authenticator);
                }

                connection = url.openConnection(http_proxy);
            } else {
                connection = url.openConnection();
            }

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

    private static List<String> getProxyHostInfo(String proxyUrl) {
        if (proxyUrl == null) {
            return null;
        }

        // matching pattern
        //  http://(host):(port) or http://(user):(pass)@(host):(port)
        //  https://(host):(port) or https://(user):(pass)@(host):(port)
        Pattern p = Pattern.compile("^(http|https):\\/\\/(([^:\\@]+):([^\\@]+)\\@)?([^:\\@\\/]+):([0-9]+)\\/?$");
        Matcher m = p.matcher(proxyUrl);
        if (!m.find()) {
            return null;
        }

        // matcher group 3:user 4:pass 5:host 6:port (null if not set)
        return Arrays.asList(m.group(5), m.group(6), m.group(3), m.group(4));
    }

    private void error(String format, String... args) {
        if (!format.endsWith("\n")) {
            format += '\n';
        }
        System.err.printf(format, args);
        System.exit(1);
    }
}
