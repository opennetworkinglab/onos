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
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.REQUIRE_INSTANCE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidBooleanValue;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * require-instance-stmt = require-instance-keyword sep
 *                            require-instance-arg-str stmtend
 *
 * require-instance-arg-str = < a string that matches the rule
 *                            require-instance-arg >
 *
 * require-instance-arg = true-keyword / false-keyword
 *
 * ANTLR grammar rule
 *
 * requireInstanceStatement : REQUIRE_INSTANCE_KEYWORD requireInstance STMTEND;
 *
 * requireInstance : string;
 */

/**
 * Represents listener based call back function corresponding to the
 * "require-instance" rule defined in ANTLR grammar file for corresponding ABNF rule
 * in RFC 6020.
 */
public final class RequireInstanceListener {

    /**
     * Creates a new require instance listener.
     */
    private RequireInstanceListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (require-instance), performs validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processRequireInstanceEntry(TreeWalkListener listener,
            GeneratedYangParser.RequireInstanceStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, REQUIRE_INSTANCE_DATA, "", ENTRY);

        Parsable curData = listener.getParsedDataStack().peek();

        // Gets the status of require instance
        boolean isRequireInstance = getValidBooleanValue(ctx.requireInstance().getText(), REQUIRE_INSTANCE_DATA, ctx);

        // Checks the holder of require-instance as leafref or type, else throws error.
        if (curData instanceof YangLeafRef) {

            // Sets the require-instance status to leafref.
            ((YangLeafRef) curData).setRequireInstance(isRequireInstance);
        } else if (curData instanceof YangType) {

            // Checks type should be instance-identifier, else throw error.
            if (((YangType) curData).getDataType() == YangDataTypes.INSTANCE_IDENTIFIER) {

                // Sets the require-instance status to instance-identifier type.
                ((YangType) curData).setDataTypeExtendedInfo(isRequireInstance);
            } else {
                throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, REQUIRE_INSTANCE_DATA,
                        ctx.getText(), ENTRY));
            }
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, REQUIRE_INSTANCE_DATA,
                    ctx.getText(), ENTRY));
        }
    }
}
