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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Simple abstraction of a Java package for the purpose of tracking
 * dependencies and requirements.
 *
 * @author Thomas Vachuska
 */
public class JavaPackage extends JavaEntity {

    private final Set<JavaSource> sources = new HashSet<>();
    private Set<JavaPackage> dependencies;

    /**
     * Creates a new Java package.
     *
     * @param name java package file name
     */
    JavaPackage(String name) {
        super(name);
    }

    /**
     * Returns the set of sources contained in this Java package.
     *
     * @return set of Java sources
     */
    public Set<JavaSource> getSources() {
        return Collections.unmodifiableSet(sources);
    }

    /**
     * Adds the specified Java source to the package. Only possible if the
     * Java package of the source is the same as this Java package.
     *
     * @param source Java source to be added
     */
    void addSource(JavaSource source) {
        if (source.getPackage().equals(this)) {
            sources.add(source);
        }
    }

    /**
     * Returns the set of packages directly required by this package.
     *
     * @return set of Java package dependencies
     */
    Set<JavaPackage> getDependencies() {
        return dependencies;
    }

    /**
     * Sets the set of resolved Java packages on which this package dependens.
     *
     * @param dependencies set of resolved Java packages
     */
    void setDependencies(Set<JavaPackage> dependencies) {
        if (this.dependencies == null) {
            this.dependencies = Collections.unmodifiableSet(new HashSet<>(dependencies));
        }
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name())
                .add("sources", sources.size())
                .add("dependencies", (dependencies != null ? dependencies.size() : 0))
                .toString();
    }

}
