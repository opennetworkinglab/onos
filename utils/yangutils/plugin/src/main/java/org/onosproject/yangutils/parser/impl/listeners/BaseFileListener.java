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

package org.onosproject.yangutils.parser.impl.listeners;

import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.YANGBASE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_CHILD;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsEmpty;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ANTLR grammar rule
 * yangfile : module_stmt
 *          | submodule_stmt;
 */

/**
 * Representation of call back function corresponding to the "base rule" defined in
 * ANTLR grammar file.
 */
public final class BaseFileListener {

    /**
     * Creates a new base listener.
     */
    private BaseFileListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (yangfile), perform validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processYangFileEntry(TreeWalkListener listener, GeneratedYangParser.YangfileContext ctx) {

        // Check if stack is empty.
        checkStackIsEmpty(listener, INVALID_HOLDER, YANGBASE_DATA, "", ENTRY);

    }

    /**
     * It is called when parser exits from grammar rule (yangfile), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processYangFileExit(TreeWalkListener listener, GeneratedYangParser.YangfileContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, YANGBASE_DATA, "", EXIT);

        // Data Model tree root node is set.
        if (listener.getParsedDataStack().peek() instanceof YangModule
                || listener.getParsedDataStack().peek() instanceof YangSubModule) {
            listener.setRootNode((YangNode) listener.getParsedDataStack().pop());
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_CHILD, YANGBASE_DATA, "", EXIT));
        }

        // Check if stack is empty.
        checkStackIsEmpty(listener, INVALID_HOLDER, YANGBASE_DATA, "", EXIT);
    }
}
