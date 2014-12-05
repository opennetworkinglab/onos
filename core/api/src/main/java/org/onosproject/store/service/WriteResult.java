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

import com.google.common.base.MoreObjects;


/**
 * Database write result.
 */
public class WriteResult {

    private final WriteStatus status;
    private final VersionedValue previousValue;

    public WriteResult(WriteStatus status, VersionedValue previousValue) {
        this.status = status;
        this.previousValue = previousValue;
    }

    public VersionedValue previousValue() {
        return previousValue;
    }

    public WriteStatus status() {
        return status;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("status", status)
                .add("previousValue", previousValue)
                .toString();
    }
}
