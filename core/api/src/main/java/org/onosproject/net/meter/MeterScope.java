/*
 * Copyright 2021-present Open Networking Foundation
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

import org.onlab.util.Identifier;

/**
 * Scope of Meter Features.
 *
 * There are multiple meter tables in a P4RT device,
 * to distinguish and represent them uniquely,
 * we added a Scope field.
 *
 * For P4RT device, value will be the PiMeterId.
 * For OF device, we use "global" by default, since there is only 1 table in OF.
 * In general, a Meter Scope is referring to a Meter Table.
 * It can be a PiMeterId or "global" for the single OF meter table.
 *
 * During runtime, users need to provide a PiMeterId to indicate which Meter Cell they
 * are intended to modify. The value will then be used to create a Meter Scope
 * for the rest of the process.
 * If no PiMeterId is provided, a "global" Meter Scope is created.
 */
public class MeterScope extends Identifier<String> {

    public static final String METER_GLOBAL_SCOPE = "global";

    /**
     * Create a Meter Scope from id string.
     * @param scope the scope
     * @return a Meter Scope
     */
    public static MeterScope of(String scope) {
        return new MeterScope(scope);
    }

    /**
     * Create a global Meter Scope.
     * @return a Meter Scope
     */
    public static MeterScope globalScope() {
        return new MeterScope(METER_GLOBAL_SCOPE);
    }

    MeterScope(String scope) {
        super(scope);
    }

    /**
     * Global scope or not.
     * @return true if global scope, false if not.
     */
    public boolean isGlobal() {
        return identifier.equals(METER_GLOBAL_SCOPE);
    }
}