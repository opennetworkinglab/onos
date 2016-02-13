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

package org.onosproject.yangutils.parser.impl.listeners;

import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.parser.ParsableDataType;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ANTLR grammar rule
 * yangfile : module_stmt
 *          | submodule_stmt;
 */

/**
 * Implements call back function corresponding to the "base rule" defined in
 * ANTLR grammar file.
 */
public final class BaseFileListener {

    /**
     * Creates a new base listener.
     */
    private BaseFileListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (yangfile), perform validations and update the data model
     * tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processYangFileEntry(TreeWalkListener listener, GeneratedYangParser.YangfileContext ctx) {

        // Check if stack is empty.
        ListenerValidation.checkStackIsEmpty(listener, ListenerErrorType.INVALID_HOLDER,
                                             ParsableDataType.YANGBASE_DATA, "", ListenerErrorLocation.ENTRY);

    }

    /**
     * It is called when parser exits from grammar rule (yangfile), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processYangFileExit(TreeWalkListener listener, GeneratedYangParser.YangfileContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                                                ParsableDataType.YANGBASE_DATA, "", ListenerErrorLocation.EXIT);

        // Data Model tree root node is set.
        if (listener.getParsedDataStack().peek() instanceof YangModule
                | listener.getParsedDataStack().peek() instanceof YangSubModule) {
            listener.setRootNode((YangNode) listener.getParsedDataStack().pop());
        } else {
            throw new ParserException(
                                      ListenerErrorMessageConstruction
                                              .constructListenerErrorMessage(ListenerErrorType.INVALID_CHILD,
                                                                             ParsableDataType.YANGBASE_DATA, "",
                                                                             ListenerErrorLocation.EXIT));
        }

        // Check if stack is empty.
        ListenerValidation.checkStackIsEmpty(listener, ListenerErrorType.INVALID_HOLDER,
                                             ParsableDataType.YANGBASE_DATA, "", ListenerErrorLocation.EXIT);
    }
}