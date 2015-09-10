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

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Abstraction of a dependency segment.
 *
 * @author Thomas Vachuska
 */
public class Dependency {

    private final JavaPackage source;
    private final JavaPackage target;

    /**
     * Creates a dependency from the specified source on the given target.
     *
     * @param source source of the dependency
     * @param target target of the dependency
     */
    public Dependency(JavaPackage source, JavaPackage target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Returns the dependency source.
     *
     * @return source Java package
     */
    public JavaPackage getSource() {
        return source;
    }

    /**
     * Returns the dependency target.
     *
     * @return target Java package
     */
    public JavaPackage getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Dependency) {
            Dependency that = (Dependency) obj;
            return Objects.equals(source, that.source) &&
                    Objects.equals(target, that.target);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("source", source).add("target", target).toString();
    }

}
