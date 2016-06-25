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

package org.onosproject.yangutils.parser.impl.parserutils;

import org.onosproject.yangutils.datamodel.CollisionDetector;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;

/**
 * Represents the detector of YANG construct collision in a YANG file.
 */
public final class ListenerCollisionDetector {

    /**
     * Creates a new listener collision.
     */
    private ListenerCollisionDetector() {
    }

    /**
     * Detects that the identifiers of all these child nodes must be unique
     * within all cases in a choice.
     *
     * @param listener listener's object
     * @param line line of identifier in YANG file, required for error
     *            reporting
     * @param charPosition character position of identifier in YANG file,
     *            required for error reporting
     * @param identifierName name for which uniqueness is to be detected
     * @param constructType type of YANG construct for which collision check is
     *            to be performed
     * @throws ParserException if identifier is not unique
     */
    public static void detectCollidingChildUtil(TreeWalkListener listener, int line, int charPosition,
            String identifierName, YangConstructType constructType)
            throws ParserException {

        if (listener.getParsedDataStack().peek() instanceof CollisionDetector) {
            try {
                ((CollisionDetector) listener.getParsedDataStack().peek()).detectCollidingChild(
                        identifierName, constructType);
            } catch (DataModelException e) {
                ParserException parserException = new ParserException(e.getMessage());
                parserException.setLine(line);
                parserException.setCharPosition(charPosition);
                throw parserException;
            }
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, constructType, identifierName,
                    EXIT));
        }
    }
}
