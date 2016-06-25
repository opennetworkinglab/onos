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
import com.google.common.base.Objects;

/**
 * Base implementation of an annotated model description.
 */
public abstract class AbstractDescription implements Annotated {

    private static final SparseAnnotations EMPTY = DefaultAnnotations.builder().build();

    private final SparseAnnotations annotations;

    // For serialization
    protected AbstractDescription() {
        this.annotations = null;
    }

    /**
     * Creates a new entity, annotated with the specified annotations.
     *
     * @param annotations optional key/value annotations map
     */
    protected AbstractDescription(SparseAnnotations... annotations) {
        checkArgument(annotations.length <= 1, "Only one set of annotations is expected");
        this.annotations = annotations.length == 1 ? annotations[0] : EMPTY;
    }

    @Override
    public SparseAnnotations annotations() {
        return annotations;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(annotations);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof AbstractDescription) {
            AbstractDescription that = (AbstractDescription) object;
            return Objects.equal(this.annotations, that.annotations);
        }
        return false;
    }

}
