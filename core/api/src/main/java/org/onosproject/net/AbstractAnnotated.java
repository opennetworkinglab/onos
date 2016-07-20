/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.DefaultAnnotations.EMPTY;

/**
 * Base abstraction of an annotated entity.
 */
public abstract class AbstractAnnotated implements Annotated {

    private final Annotations annotations;

    // For serialization
    protected AbstractAnnotated() {
        this.annotations = null;
    }

    /**
     * Creates a new entity, annotated with the specified annotations.
     *
     * @param annotations optional key/value annotations map
     */
    protected AbstractAnnotated(Annotations... annotations) {
        checkArgument(annotations.length <= 1, "Only one set of annotations is expected");
        this.annotations = annotations.length == 1 ? annotations[0] : EMPTY;
    }

    @Override
    public Annotations annotations() {
        return annotations;
    }

}
