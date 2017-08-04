/*
 * Copyright 2017-present Open Networking Foundation
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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * MarkerResource to add hints about installable Intent.
 */
public final class MarkerResource implements NetworkResource {
    private final String mark;

    private MarkerResource(String mark) {
        this.mark = checkNotNull(mark);
    }

    /**
     * Creates an instance of MarkerResource.
     *
     * @param mark marker String
     * @return MarkerResource
     */
    public static MarkerResource marker(String mark) {
        return new MarkerResource(mark);
    }

    @Override
    public int hashCode() {
        return mark.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MarkerResource) {
            return mark.equals(((MarkerResource) obj).mark);
        }
        return false;
    }

    @Override
    public String toString() {
        return mark;
    }


    private MarkerResource() {
        this.mark = "";
    }
}
