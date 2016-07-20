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
import com.google.common.collect.ImmutableBiMap;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;

/**
 * A BMv2 configuration interpreter.
 */
@Beta
public interface Bmv2Interpreter {

    /**
     * Returns a bi-map describing a one-to-one relationship between ONOS flow rule table IDs and BMv2 table names.
     *
     * @return a {@link com.google.common.collect.BiMap} where the key is a ONOS flow rule table id and
     * the value is a BMv2 table names
     */
    ImmutableBiMap<Integer, String> tableIdMap();

    /**
     * Returns a bi-map describing a one-to-one relationship between ONOS criterion types and BMv2 header field names.
     * Header field names are formatted using the notation {@code header_name.field_member_name}.
     *
     * @return a {@link com.google.common.collect.BiMap} where the keys are ONOS criterion types and the values are
     * BMv2 header field names
     */
    ImmutableBiMap<Criterion.Type, String> criterionTypeMap();

    /**
     * Return a BMv2 action that is functionally equivalent to the given ONOS traffic treatment for the given
     * configuration.
     *
     * @param treatment     a ONOS traffic treatment
     * @param configuration a BMv2 configuration
     * @return a BMv2 action object
     * @throws Bmv2InterpreterException if the treatment cannot be mapped to any BMv2 action
     */
    Bmv2Action mapTreatment(TrafficTreatment treatment, Bmv2Configuration configuration)
            throws Bmv2InterpreterException;

}
