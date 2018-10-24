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

import java.util.List;

/**
 * Representation of a java library for Bazel.
 */
public class BazelLibrary {

    private final String name;
    private final List<String> targets;

    public static BazelLibrary getLibrary(String libraryName, List<String> libraryTargets) {
        return new BazelLibrary(libraryName, libraryTargets);
    }

    private BazelLibrary(String name, List<String> targets) {
        this.name = name;
        this.targets = targets;
    }

    private String normalizeName(String name) {
        if (!name.startsWith("//")) {
            return name.replaceAll("[.-]", "_");
        } else {
            return name;
        }
    }

    private String convertTargetName(String targetName) {
        return normalizeName((targetName.startsWith("//") ?
                targetName : targetName.replaceFirst(":", "@")));
    }

    private boolean isAllUpper(String s) {
        return s.toUpperCase().equals(s);
    }

    public String name() {
        return normalizeName(name);
    }

    public String getFragment() {
        StringBuilder output = new StringBuilder()
                .append(name())
                .append(" = [");

        targets.forEach(target -> {
            if (isAllUpper(target)) {
                output.append(String.format("] + %s + [", target.replaceFirst(":", "")));
            } else {
                String pathEnd = target.startsWith("//") ? "" : "//jar";
                output.append(String.format("\n    \"%s%s\",", convertTargetName(target), pathEnd));
            }
        });
        output.append("\n]\n");

        return output.toString();
    }

}