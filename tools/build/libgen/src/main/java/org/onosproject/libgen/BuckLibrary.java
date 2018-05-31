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

import org.eclipse.aether.artifact.Artifact;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representation of a java library for Buck.
 */
public class BuckLibrary {

    private final String name;
    private final List<String> targets;
    private final boolean generateForBazel;

    private final Set<Artifact> provided = new HashSet<>();
    private final Set<Artifact> runtime = new HashSet<>();

    public static BuckLibrary getLibrary(String libraryName, List<String> libraryTargets, boolean generateForBazel) {
        return new BuckLibrary(libraryName, libraryTargets, generateForBazel);
    }

    private BuckLibrary(String name, List<String> targets, boolean generateForBazel) {
        this.name = name;
        this.targets = targets;
        this.generateForBazel = generateForBazel;
    }

    private String normalizeName(String name) {
        if (!name.startsWith("//")) {
            return name.replaceAll("[.-]", "_");
        } else {
            return name;
        }
    }

    private String convertBuckTargetName(String buckTargetName) {
        return normalizeName((buckTargetName.startsWith("//") ?
                buckTargetName : buckTargetName.replaceFirst(":", "@")));
    }

    private boolean isAllUpper(String s) {
        return s.toUpperCase().equals(s);
    }

    public String name() {
        if (!generateForBazel) {
            return name;
        } else {
            return normalizeName(name);
        }
    }

    public String getFragment() {
        if (generateForBazel) {
            return getBazelFragment();
        } else {
            return getBuckFragment();
        }
    }

    private String getBazelFragment() {
        StringBuilder output = new StringBuilder()
                .append(name())
                .append(" = [");

        targets.forEach(target -> {
            if (isAllUpper(target)) {
                output.append(String.format("] + %s + [", target.replaceFirst(":", "")));
            } else {
                String pathEnd = target.startsWith("//") ? "" : "//jar";
                output.append(String.format("\n    \"%s%s\",", convertBuckTargetName(target), pathEnd));
            }
        });
        output.append("\n]\n");

        return output.toString();
    }

    private String getBuckFragment() {
        StringBuilder output = new StringBuilder()
                .append("osgi_feature_group(\n")
                .append(String.format("  name = '%s',\n", name))
                .append("  visibility = ['PUBLIC'],\n")
                .append("  exported_deps = [");

        targets.forEach(target -> output.append(String.format("\n    '%s',", target)));
        output.append("\n  ],\n)\n\n");

        return output.toString();
    }
}