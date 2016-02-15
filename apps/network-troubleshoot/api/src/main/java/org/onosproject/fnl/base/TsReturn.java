/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.fnl.base;

/**
 * Represents an additional value that a caller may pass to a method.
 * Be filled in by the called method.
 *
 * Used as an extra return value.
 *
 * @param <M> the class of expected return value
 */
public final class TsReturn<M> {
    private M ret;

    /**
     * Sets the value of this instance.
     *
     * @param value the value to set
     */
    public void setValue(M value) {
        ret = value;
    }

    /**
     * Returns the value of this instance.
     *
     * @return the value
     */
    public M getValue() {
        return ret;
    }

    /**
     * Returns true if the value has been set.
     * Generally, if setValue() has not been invoked,
     * the value will not be present (i.e. null).
     *
     * @return true, if ret is present;
     *         false, otherwise
     */
    public boolean isPresent() {
        return ret != null;
    }
}
