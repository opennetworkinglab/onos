/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.api.context;

import com.google.common.annotations.Beta;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntry;
import org.onosproject.net.flow.FlowRule;

/**
 * Translator of ONOS flow rules to BMv2 table entries.
 */
@Beta
public interface Bmv2FlowRuleTranslator {

    /**
     * Returns a BMv2 table entry equivalent to the given flow rule for the given context.
     * <p>
     * Translation is performed according to the following logic:
     * <ul>
     *  <li> table name: obtained from the context interpreter {@link Bmv2Interpreter#tableIdMap() table ID map}.
     *  <li> match key: is built using both the context interpreter {@link Bmv2Interpreter#criterionTypeMap() criterion
     *  map} and all {@link org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector extension selectors} (if any).
     *  <li> action: is built using the context interpreter
     *          {@link Bmv2Interpreter#mapTreatment(org.onosproject.net.flow.TrafficTreatment, Bmv2Configuration)
     *          treatment mapping function} or the flow rule
     *          {@link org.onosproject.bmv2.api.runtime.Bmv2ExtensionTreatment extension treatment} (if any).
     *  <li> timeout: if the table supports timeout, use the same as the flow rule, otherwise none (i.e. returns a
     *          permanent entry).
     * </ul>
     *
     * @param rule    a flow rule
     * @param context a context
     * @return a BMv2 table entry
     * @throws Bmv2FlowRuleTranslatorException if the flow rule cannot be translated
     */
    Bmv2TableEntry translate(FlowRule rule, Bmv2DeviceContext context) throws Bmv2FlowRuleTranslatorException;
}
