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

/**
 * A context permitting the application to be notified when the
 * meter installation has been successful.
 */
public interface MeterContext {

    /**
     * Invoked on successful installation of the meter.
     *
     * @param op a meter
     */
    default void onSuccess(MeterRequest op) {}

    /**
     * Invoked when error is encountered while installing a meter.
     *
     * @param op a meter
     * @param reason the reason why it failed
     */
    default void onError(MeterRequest op, MeterFailReason reason) {}
}
