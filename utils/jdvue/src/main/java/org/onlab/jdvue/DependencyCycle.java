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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Simple representation of a Java package dependency cycle.
 */
public class DependencyCycle {

    private final List<JavaPackage> cycle;

    /**
     * Creates a normalized dependency cycle represented by the specified list
     * of Java packages, which are expected to be given in order of dependency.
     * List is assumed to be non-empty.
     *
     * @param cycle list of Java packages in the dependency cycle
     * @param cause Java package that caused the cycle
     */
    DependencyCycle(List<JavaPackage> cycle, JavaPackage cause) {
        this.cycle = normalize(cycle, cause);
    }

    /**
     * Produces a normalized dependency cycle list. Normalization is performed
     * by rotating the list so that the package with the least lexicographic
     * name is at the start of the list.
     *
     * @param cycle list of Java packages in the dependency cycle
     * @param cause Java package that caused the cycle
     * @return normalized cycle
     */
    private List<JavaPackage> normalize(List<JavaPackage> cycle, JavaPackage cause) {
        int start = cycle.indexOf(cause);
        List<JavaPackage> clone = new ArrayList<>(cycle.subList(start, cycle.size()));
        int leastIndex = findIndexOfLeastName(clone);
        Collections.rotate(clone, -leastIndex);
        return Collections.unmodifiableList(clone);
    }

    /**
     * Returns the index of the Java package with the least name.
     *
     * @param cycle list of Java packages in the dependency cycle
     * @return index of the least Java package name
     */
    private int findIndexOfLeastName(List<JavaPackage> cycle) {
        int leastIndex = 0;
        String leastName = cycle.get(leastIndex).name();
        for (int i = 1, n = cycle.size(); i < n; i++) {
            JavaPackage javaPackage = cycle.get(i);
            if (leastName.compareTo(javaPackage.name()) > 0) {
                leastIndex = i;
                leastName = javaPackage.name();
            }
        }
        return leastIndex;
    }

    /**
     * Returns the normalized Java package dependency cycle
     *
     * @return list of packages in the dependency cycle
     */
    public List<JavaPackage> getCycle() {
        return cycle;
    }

    /**
     * Returns the dependency cycle in form of individual dependencies.
     *
     * @return list of dependencies forming the cycle
     */
    public List<Dependency> getCycleSegments() {
        List<Dependency> dependencies = new ArrayList<>();
        for (int i = 0, n = cycle.size(); i < n; i++) {
            dependencies.add(new Dependency(cycle.get(i), cycle.get(i < n - 1 ? i + 1 : 0)));
        }
        return dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DependencyCycle) {
            DependencyCycle that = (DependencyCycle) o;
            return Objects.equals(cycle, that.cycle);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return cycle.hashCode();
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("cycle", cycle).toString();
    }

    public String toShortString() {
        StringBuilder sb = new StringBuilder("[");
        for (JavaPackage javaPackage : cycle) {
            sb.append(javaPackage.name()).append(", ");
        }
        if (sb.length() > 1) {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("]");
        return sb.toString();
    }

}
