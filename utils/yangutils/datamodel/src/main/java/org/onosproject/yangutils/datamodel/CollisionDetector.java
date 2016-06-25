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

package org.onosproject.yangutils.datamodel;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

/**
 * Abstraction of YANG collision function. Abstracted to unify the collision
 * detection functionality.
 */
public interface CollisionDetector {
    /**
     * Checks for the colliding child.
     *
     * @param identifierName name of identifier for which collision to be
     * checked
     * @param dataType type of the YANG construct for which collision to be
     * checked
     * @throws DataModelException if there is any collision in YANG rules in
     *             parsed data, corresponding exception should be thrown
     */
    void detectCollidingChild(String identifierName, YangConstructType dataType)
            throws DataModelException;

    /**
     * Check for the self collision.
     *
     * @param identifierName name of identifier for which collision to be
     * checked
     * @param dataType type of the YANG construct for which collision to be
     * checked
     * @throws DataModelException if there is any collision in YANG rules in
     *                            parsed data, corresponding exception should be thrown
     */
    void detectSelfCollision(String identifierName, YangConstructType dataType)
            throws DataModelException;
}
