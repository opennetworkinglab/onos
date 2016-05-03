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
import org.onosproject.net.flow.criteria.Criterion;

import java.util.Map;

import static org.onosproject.drivers.bmv2.translators.Bmv2FlowRuleTranslator.TranslatorConfig;

/**
 * Default implementation of a BMv2 flow rule translator configuration.
 */
@Beta
public abstract class Bmv2DefaultTranslatorConfig implements TranslatorConfig {

    private final Bmv2Model model;
    private final Map<String, Criterion.Type> fieldMap;

    /**
     * Creates a new translator configuration.
     *
     * @param model    a BMv2 packet processing model
     * @param fieldMap a field-to-criterion type map
     */
    protected Bmv2DefaultTranslatorConfig(Bmv2Model model, Map<String, Criterion.Type> fieldMap) {
        this.model = model;
        this.fieldMap = fieldMap;
    }

    @Override
    public Bmv2Model model() {
        return this.model;
    }

    @Override
    public Map<String, Criterion.Type> fieldToCriterionTypeMap() {
        return this.fieldMap;
    }
}
