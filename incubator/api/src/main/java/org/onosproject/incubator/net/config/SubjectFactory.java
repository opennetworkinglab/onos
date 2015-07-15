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
package org.onosproject.incubator.net.config;


import com.google.common.annotations.Beta;

/**
 * Base abstract factory for creating configuration subjects from their
 * string key image.
 *
 * @param <S> subject class
 */
@Beta
public abstract class SubjectFactory<S> {

    private final Class<S> subjectClass;
    private final String subjectKey;

    /**
     * Creates a new configuration factory for the specified class of subjects
     * capable of generating the configurations of the specified class. The
     * subject and configuration class keys are used merely as keys for use in
     * composite JSON trees.
     *
     * @param subjectClass subject class
     * @param subjectKey   subject class key
     */
    protected SubjectFactory(Class<S> subjectClass, String subjectKey) {
        this.subjectClass = subjectClass;
        this.subjectKey = subjectKey;
    }

    /**
     * Returns the class of the subject to which this factory applies.
     *
     * @return subject type
     */
    public Class<S> subjectClass() {
        return subjectClass;
    }

    /**
     * Returns the unique key of this configuration subject class.
     * This is primarily aimed for use in composite JSON trees in external
     * representations and has no bearing on the internal behaviours.
     *
     * @return configuration key
     */
    public String subjectKey() {
        return subjectKey;
    }

    /**
     * Creates a configuration subject from its key image.
     *
     * @return configuration subject
     */
    public abstract S createSubject(String key);

}
