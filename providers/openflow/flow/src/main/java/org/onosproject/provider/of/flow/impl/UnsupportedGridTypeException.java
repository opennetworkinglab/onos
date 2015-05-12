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
package org.onosproject.provider.of.flow.impl;

import org.onosproject.net.GridType;

/**
 * Thrown to indicate that unsupported gird type is referred.
 */
public class UnsupportedGridTypeException extends RuntimeException {

    /**
     * Creates an instance with the specified unsupported grid type.
     *
     * @param unsupported unsupported grid type
     */
    public UnsupportedGridTypeException(GridType unsupported) {
        super("GridType " + unsupported + " is not supported");
    }
}
