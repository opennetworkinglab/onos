/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.meter;

import java.util.Optional;

/**
 * An entity used to indicate whether the store operation passed.
 */
public final class MeterStoreResult {


    private final Type type;
    private final Optional<MeterFailReason> reason;

    public enum Type {
        SUCCESS,
        FAIL
    }

    private MeterStoreResult(Type type, MeterFailReason reason) {
        this.type = type;
        this.reason = Optional.ofNullable(reason);
    }

    public Type type() {
        return type;
    }

    public Optional<MeterFailReason> reason() {
        return reason;
    }

    /**
     * A successful store opertion.
     *
     * @return a meter store result
     */
    public static MeterStoreResult success() {
        return new MeterStoreResult(Type.SUCCESS, null);
    }

    /**
     * A failed store operation.
     *
     * @param reason a failure reason
     * @return a meter store result
     */
    public static MeterStoreResult fail(MeterFailReason reason) {
        return new MeterStoreResult(Type.FAIL, reason);
    }

}
