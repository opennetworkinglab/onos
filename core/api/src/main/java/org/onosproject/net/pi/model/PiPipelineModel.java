/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.pi.model;

import com.google.common.annotations.Beta;

import java.util.Collection;
import java.util.Optional;

/**
 * Model of a protocol-independent pipeline.
 */
@Beta
public interface PiPipelineModel {

    /**
     * Returns the header type associated with the given name, if present.
     *
     * @param name string value
     * @return optional header type model
     */
    Optional<PiHeaderTypeModel> headerType(String name);

    /**
     * Returns the collection of all header types defined by this pipeline model.
     *
     * @return collection of header types
     */
    Collection<PiHeaderTypeModel> headerTypes();

    /**
     * Returns the header instance associated with the given name, if present.
     *
     * @param name string value
     * @return optional header instance model
     */
    Optional<PiHeaderModel> header(String name);

    /**
     * Returns the collection of all header instance models defined by this pipeline model.
     *
     * @return collection of header types
     */
    Collection<PiHeaderModel> headers();

    /**
     * Returns the action model associated with the given name, if present.
     *
     * @param name string value
     * @return optional action model
     */
    Optional<PiActionModel> action(String name);

    /**
     * Returns the collection of all action models defined by this pipeline model.
     *
     * @return collection of actions
     */
    Collection<PiActionModel> actions();

    /**
     * Returns the table model associated with the given name, if present.
     *
     * @param name string value
     * @return optional table model
     */
    Optional<PiTableModel> table(String name);

    /**
     * Returns the collection of all table models defined by this pipeline model.
     *
     * @return collection of actions
     */
    Collection<PiTableModel> tables();
}
