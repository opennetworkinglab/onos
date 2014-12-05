/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.service;

/**
 * A container object which either has a result or an exception.
 * <p>
 * If a result is present, get() will return it otherwise get() will throw
 * the exception that was encountered in the process of generating the result.
 * </p>
 * @param <R> type of result.
 * @param <E> exception encountered in generating the result.
 */
public interface OptionalResult<R, E extends Throwable> {

    /**
     * Returns the result or throws an exception if there is no
     * valid result.
     * @return result
     */
    public R get();

    /**
     * Returns true if there is a valid result.
     * @return true is yes, false otherwise.
     */
    public boolean hasValidResult();
}
