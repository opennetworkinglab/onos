/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.artemis.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.json.JSONArray;
import org.json.JSONException;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Artemis Configuration Class.
 */
class ArtemisConfig extends Config<ApplicationId> {

    private static final String PREFIXES = "prefixes";
    /* */
    private static final String PREFIX = "prefix";
    private static final String PATHS = "paths";
    private static final String MOAS = "moas";
    /* */
    private static final String ORIGIN = "origin";
    private static final String NEIGHBOR = "neighbor";
    private static final String ASN = "asn";
    /* */

    private static final String MONITORS = "monitors";
    /* */
    private static final String RIPE = "ripe";
    private static final String EXABGP = "exabgp";
    /* */

    private static final String FREQUENCY = "frequency";


    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Gets the set of monitored prefixes with the details (prefix, paths and MOAS).
     *
     * @return artemis class prefixes
     */
    Set<ArtemisPrefixes> monitoredPrefixes() {
        Set<ArtemisPrefixes> prefixes = Sets.newHashSet();

        JsonNode prefixesNode = object.get(PREFIXES);
        if (prefixesNode == null) {
            log.warn("prefixes field is null!");
            return prefixes;
        }

        prefixesNode.forEach(jsonNode -> {
            IpPrefix prefix = IpPrefix.valueOf(jsonNode.get(PREFIX).asText());

            Set<Integer> moasNumbers = Sets.newHashSet();
            JsonNode moasNode = jsonNode.get(MOAS);
            moasNode.forEach(asn ->
                    moasNumbers.add(asn.asInt())
            );

            /*
            "paths" : [{
                "origin" : 65004,
                "neighbor" : [{
                        "asn" : 65002,
                        "neighbor": [{
                            "asn" : 65001,
                        }]
                }]
            }]
            */

            Map<Integer, Map<Integer, Set<Integer>>> paths = Maps.newHashMap();
            JsonNode pathsNode = jsonNode.get(PATHS);
            pathsNode.forEach(path -> {
                addPath(paths, path);
            });

            // printPaths(paths);

            prefixes.add(new ArtemisPrefixes(prefix, moasNumbers, paths));
        });

        return prefixes;
    }

    /**
     * Appends an ASN path on the ASN paths list of the Artemis application.
     *
     * @param paths active ASN paths list
     * @param path  ASN path to be added
     */
    private void addPath(Map<Integer, Map<Integer, Set<Integer>>> paths, JsonNode path) {
        Integer origin = path.path(ORIGIN).asInt();

        JsonNode firstNeighborNode = path.path(NEIGHBOR);
        // Check if neighbor exists in the configuration
        if (!firstNeighborNode.isMissingNode()) {
            firstNeighborNode.forEach(firstNeighbor -> {
                Integer firstNeighborAsn = firstNeighbor.get(ASN).asInt();

                JsonNode secondNeighborNode = firstNeighbor.path(NEIGHBOR);
                // check if second neighbor exists in configuration
                if (!secondNeighborNode.isMissingNode()) {
                    secondNeighborNode.forEach(secondNeighbor -> {
                        Integer secondNeighborAsn = secondNeighbor.asInt();

                        if (paths.containsKey(origin)) {
                            // paths already contain origin ASN.
                            Map<Integer, Set<Integer>> integerSetMap = paths.get(origin);
                            if (integerSetMap.containsKey(firstNeighborAsn)) {
                                integerSetMap.get(firstNeighborAsn).add(secondNeighborAsn);
                            } else {
                                paths.get(origin).put(firstNeighborAsn, Sets.newHashSet(secondNeighborAsn));
                            }
                        } else {
                            // origin ASN does not exist in Map.
                            Map<Integer, Set<Integer>> first2second = Maps.newHashMap();
                            first2second.put(firstNeighborAsn, Sets.newHashSet(secondNeighborAsn));
                            paths.put(origin, first2second);
                        }
                    });
                // else append to paths without second neighbor
                } else {
                    if (!paths.containsKey(origin)) {
                        Map<Integer, Set<Integer>> first2second = Maps.newHashMap();
                        first2second.put(firstNeighborAsn, Sets.newHashSet());
                        paths.put(origin, first2second);
                    } else {
                        // paths already contain origin ASN.
                        Map<Integer, Set<Integer>> integerSetMap = paths.get(origin);
                        if (!integerSetMap.containsKey(firstNeighborAsn)) {
                            paths.get(origin).put(firstNeighborAsn, Sets.newHashSet());
                        }
                    }
                }
            });
        // else append to paths only the origin
        } else {
            if (!paths.containsKey(origin)) {
                paths.put(origin, Maps.newHashMap());
            }
        }
    }

    /**
     * Helper function to print the loaded ASN paths.
     *
     * @param paths ASN paths to print
     */
    private void printPaths(Map<Integer, Map<Integer, Set<Integer>>> paths) {
        log.warn("------------------------------------");
        paths.forEach((k, v) -> v.forEach((l, n) -> {
            n.forEach(p -> log.warn("Origin: " + k + ", 1st: " + l + ", 2nd: " + p));
        }));
    }

    /**
     * Gets the frequency of the detection module in milliseconds.
     *
     * @return frequency (ms)
     */
    int detectionFrequency() {
        JsonNode thresholdNode = object.get(FREQUENCY);
        int threshold = 0;

        if (thresholdNode == null) {
            log.warn("threshold field is null!");
            return threshold;
        }

        return thresholdNode.asInt();
    }

    /**
     * Gets the active route collectors.
     *
     * @return map with type as a key and host as a value.
     */
    Map<String, Set<String>> activeMonitors() {
        Map<String, Set<String>> monitors = Maps.newHashMap();

        JsonNode monitorsNode = object.get(MONITORS);

        JsonNode ripeNode = monitorsNode.path(RIPE);
        if (!ripeNode.isMissingNode()) {
            Set<String> hosts = Sets.newHashSet();
            ripeNode.forEach(host -> hosts.add(host.asText()));
            monitors.put(RIPE, hosts);
        }

        JsonNode exabgpNode = monitorsNode.path(EXABGP);
        if (!exabgpNode.isMissingNode()) {
            Set<String> hosts = Sets.newHashSet();
            exabgpNode.forEach(host -> hosts.add(host.asText()));
            monitors.put(EXABGP, hosts);
        }

        return monitors;
    }

    /**
     * Configuration for a specific prefix.
     */
    static class ArtemisPrefixes {
        private IpPrefix prefix;
        private Set<Integer> moas;
        private Map<Integer, Map<Integer, Set<Integer>>> paths;

        private final Logger log = LoggerFactory.getLogger(getClass());

        ArtemisPrefixes(IpPrefix prefix, Set<Integer> moas, Map<Integer, Map<Integer, Set<Integer>>> paths) {
            this.prefix = checkNotNull(prefix);
            this.moas = checkNotNull(moas);
            this.paths = checkNotNull(paths);
        }

        protected IpPrefix prefix() {
            return prefix;
        }

        protected Set<Integer> moas() {
            return moas;
        }

        protected Map<Integer, Map<Integer, Set<Integer>>> paths() {
            return paths;
        }

        /**
         * Given a path we check if the origin is a friendly MOAS or our ASN.
         * If the origin ASN is not ours the we have a hijack of type 0. Next, in case that the first neighbor is
         * not a legit neighbor from our configuration we detect a hijack of type 1 and lastly, if the second
         * neighbor is not a legit neighbor we detect a type 2 hijack.
         *
         * @param path as-path that announces our prefix and found from monitors
         * @return <code>0</code> no bgp hijack detected
         *         <code>50</code> friendly anycaster announcing our prefix
         *         <code>100+i</code> BGP hijack type i (0 &lt;= i &lt;=2)
         */
        int checkPath(JSONArray path) {
            // TODO add MOAS check
            ArrayList<Integer> asnPath = new ArrayList<>();
            for (int i = 0; i < path.length(); i++) {
                try {
                    asnPath.add(path.getInt(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            // reverse the list to get path starting from origin
            Collections.reverse(asnPath);

            if (asnPath.size() > 0 && !paths.containsKey(asnPath.get(0))) {
                return 100;
            } else if (asnPath.size() > 1 && !paths.get(asnPath.get(0)).containsKey(asnPath.get(1))) {
                return 101;
            } else if (asnPath.size() > 2 && !paths.get(asnPath.get(0)).get(asnPath.get(1)).contains(asnPath.get(2))) {
                return 102;
            }
            return 0;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(prefix);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof PrefixHandler) {
                final PrefixHandler that = (PrefixHandler) obj;
                return Objects.equals(this.prefix, that.getPrefix());
            }
            return false;
        }
    }

}
