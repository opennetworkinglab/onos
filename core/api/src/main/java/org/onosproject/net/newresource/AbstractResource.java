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
package org.onosproject.net.newresource;

import com.google.common.annotations.Beta;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base implementation of a class representing resource which belongs to a particular subject.
 *
 * @param <S> type of the subject
 * @param <T> type of the resource
 */
@Beta
public abstract class AbstractResource<S, T> implements Resource<S, T> {

    private final S subject;
    private final T resource;

    /**
     * Constructor expected to be called by constructors of the sub-classes.
     *
     * @param subject identifier which this resource belongs to
     * @param resource resource of the subject
     */
    protected AbstractResource(S subject, T resource) {
        this.subject = checkNotNull(subject);
        this.resource = checkNotNull(resource);
    }

    @Override
    public S subject() {
        return subject;
    }

    @Override
    public T resource() {
        return resource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, resource);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AbstractResource)) {
            return false;
        }
        final AbstractResource that = (AbstractResource) obj;
        return Objects.equals(this.subject, that.subject)
                && Objects.equals(this.resource, that.resource);
    }
}
