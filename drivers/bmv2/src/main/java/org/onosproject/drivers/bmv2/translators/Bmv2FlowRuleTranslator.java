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

package org.onosproject.drivers.bmv2.translators;

import com.google.common.annotations.Beta;
import org.onosproject.bmv2.api.model.Bmv2Model;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;

import java.util.Map;

/**
 * Translator of ONOS flow rules to BMv2 table entries. Translation depends on a
 * {@link TranslatorConfig translator configuration}.
 */
@Beta
public interface Bmv2FlowRuleTranslator {

    /**
     * Returns a new BMv2 table entry equivalent to the given flow rule.
     *
     * @param rule a flow rule
     * @return a BMv2 table entry
     * @throws Bmv2FlowRuleTranslatorException if the flow rule cannot be
     *                                         translated
     */
    Bmv2TableEntry translate(FlowRule rule) throws Bmv2FlowRuleTranslatorException;

    /**
     * Returns the configuration of this translator.
     *
     * @return a translator configuration
     */
    TranslatorConfig config();

    /**
     * BMv2 flow rules translator configuration. Such a configuration is used to
     * generate table entries that are compatible with a given {@link Bmv2Model}.
     */
    @Beta
    interface TranslatorConfig {
        /**
         * Return the {@link Bmv2Model} associated with this configuration.
         *
         * @return a BMv2 model
         */
        Bmv2Model model();

        /**
         * Returns a map describing a one-to-one relationship between BMv2
         * header field names and ONOS criterion types. Header field names are
         * formatted using the notation {@code header_name.field_name}
         * representing a specific header field instance extracted by the BMv2
         * parser (e.g. {@code ethernet.dstAddr}).
         *
         * @return a map where the keys represent BMv2 header field names and
         * values are criterion types
         */
        Map<String, Criterion.Type> fieldToCriterionTypeMap();

        /**
         * Return a BMv2 action that is equivalent to the given ONOS traffic
         * treatment.
         *
         * @param treatment a traffic treatment
         * @return a BMv2 action object
         * @throws Bmv2FlowRuleTranslatorException if the treatment cannot be
         *                                         translated to a BMv2 action
         */
        Bmv2Action buildAction(TrafficTreatment treatment)
                throws Bmv2FlowRuleTranslatorException;
    }
}
