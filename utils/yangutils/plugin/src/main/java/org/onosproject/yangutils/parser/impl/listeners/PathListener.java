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

import org.onosproject.yangutils.datamodel.YangLeafRef;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.validatePathArgument;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.PATH_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *  leafref-specification =
 *                        ;; these stmts can appear in any order
 *                        path-stmt stmtsep
 *                        [require-instance-stmt stmtsep]
 *
 * path-stmt           = path-keyword sep path-arg-str stmtend
 *
 * ANTLR grammar rule
 *
 * leafrefSpecification : (pathStatement (requireInstanceStatement)?) | ((requireInstanceStatement)? pathStatement);
 *
 * pathStatement : PATH_KEYWORD path STMTEND;
 */

/**
 * Represents listener based call back function corresponding to the
 * "path" rule defined in ANTLR grammar file for corresponding ABNF rule
 * in RFC 6020.
 */
public final class PathListener {

    /**
     * Creates a new path listener.
     */
    private PathListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (path), performs validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processPathEntry(TreeWalkListener listener,
            GeneratedYangParser.PathStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, PATH_DATA, ctx.path().getText(), ENTRY);

        Parsable curData = listener.getParsedDataStack().peek();

        // Checks the holder of path as leafref, else throws error.
        if (curData instanceof YangLeafRef) {

            // Splitting the path argument and updating it in the datamodel tree.
            validatePathArgument(ctx.path().getText(), PATH_DATA, ctx, (YangLeafRef) curData);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, PATH_DATA,
                    ctx.path().getText(), ENTRY));
        }
    }
}
