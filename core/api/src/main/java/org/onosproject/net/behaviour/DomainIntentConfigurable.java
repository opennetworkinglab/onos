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

package org.onosproject.net.behaviour;

import org.onosproject.net.domain.DomainIntent;
import org.onosproject.net.driver.Behaviour;

import java.util.Collection;

/**
 * Behaviour to manages the intent in a network domain.
 */
public interface DomainIntentConfigurable extends Behaviour {

    /**
     * Submit a intent in a network domain.
     *
     * @param intent the domain intent to be added
     * @return the intent installed successfully
     */
    DomainIntent sumbit(DomainIntent intent);

    /**
     * Remove a intent in a domain.
     *
     * @param intent the domain intent to be removed.
     * @return the intent removed successfully
     */
    DomainIntent remove(DomainIntent intent);

    /**
     * Retrieves all installed intend network domain.
     *
     * @return a collection of intent installed
     */
    Collection<DomainIntent> getIntents();

}
