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

import com.eclipsesource.json.JsonObject;
import com.google.common.annotations.Beta;

import java.util.List;

/**
 * BMv2 packet processing configuration. Such a configuration is used to define the way BMv2 should process packets
 * (i.e. it defines the device ingress/egress pipelines, parser, tables, actions, etc.). It must be noted that this
 * class exposes only a subset of the configuration properties of a BMv2 device (only those that are needed for the
 * purpose of translating ONOS structures to BMv2 structures). Such a configuration is backed by a JSON object.
 * BMv2 JSON configuration files are usually generated using a P4 frontend compiler such as p4c-bmv2.
 */
@Beta
public interface Bmv2Configuration {

    /**
     * Return an unmodifiable view of the JSON backing this configuration.
     *
     * @return a JSON object.
     */
    JsonObject json();

    /**
     * Returns the header type associated with the given numeric ID, null if there's no such an ID in the configuration.
     *
     * @param id integer value
     * @return header type object or null
     */
    Bmv2HeaderTypeModel headerType(int id);

    /**
     * Returns the header type associated with the given name, null if there's no such a name in the configuration.
     *
     * @param name string value
     * @return header type object or null
     */
    Bmv2HeaderTypeModel headerType(String name);

    /**
     * Returns the list of all the header types defined by in this configuration. Values returned are sorted in
     * ascending order based on the numeric ID.
     *
     * @return list of header types
     */
    List<Bmv2HeaderTypeModel> headerTypes();

    /**
     * Returns the header associated with the given numeric ID, null if there's no such an ID in the configuration.
     *
     * @param id integer value
     * @return header object or null
     */
    Bmv2HeaderModel header(int id);

    /**
     * Returns the header associated with the given name, null if there's no such a name in the configuration.
     *
     * @param name string value
     * @return header object or null
     */
    Bmv2HeaderModel header(String name);

    /**
     * Returns the list of all the header instances defined in this configuration. Values returned are sorted in
     * ascending order based on the numeric ID.
     *
     * @return list of header types
     */
    List<Bmv2HeaderModel> headers();

    /**
     * Returns the action associated with the given numeric ID, null if there's no such an ID in the configuration.
     *
     * @param id integer value
     * @return action object or null
     */
    Bmv2ActionModel action(int id);

    /**
     * Returns the action associated with the given name, null if there's no such a name in the configuration.
     *
     * @param name string value
     * @return action object or null
     */
    Bmv2ActionModel action(String name);

    /**
     * Returns the list of all the actions defined by in this configuration. Values returned are sorted in ascending
     * order based on the numeric ID.
     *
     * @return list of actions
     */
    List<Bmv2ActionModel> actions();

    /**
     * Returns the table associated with the given numeric ID, null if there's no such an ID in the configuration.
     *
     * @param id integer value
     * @return table object or null
     */
    Bmv2TableModel table(int id);

    /**
     * Returns the table associated with the given name, null if there's no such a name in the configuration.
     *
     * @param name string value
     * @return table object or null
     */
    Bmv2TableModel table(String name);

    /**
     * Returns the list of all the tables defined by in this configuration. Values returned are sorted in ascending
     * order based on the numeric ID.
     *
     * @return list of actions
     */
    List<Bmv2TableModel> tables();
}
