/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.yangutils.parser.impl.parserutils;

import org.onosproject.yangutils.parser.impl.TreeWalkListener;

/**
 * Its a utility to carry out listener validation.
 */
public final class ListenerValidation {

    /**
     * Creates a new belongto listener.
     */
    private ListenerValidation() {
    }

    /**
     * Checks if error is set or parsed data stack is empty.
     *
     * @param listener Listener's object.
     * @param errNode parsable node for which validation needs to be done.
     * @return validation result.
     */
    public static boolean preValidation(TreeWalkListener listener, String errNode) {

        // Check whether error found while walking YANG file, if yes return true.
        if (listener.getErrorInformation().isErrorFlag()) {
            return true;
        }

        // If stack is empty it indicates error condition
        if (listener.getParsedDataStack().empty()) {
            listener.getErrorInformation().setErrorFlag(true);
            listener.getErrorInformation().setErrorMsg("Parsable stack empty at" + errNode + "entry");
            return true;
        }
        return false;
    }
}