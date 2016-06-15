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

import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.MANDATORY_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidBooleanValue;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *  mandatory-stmt      = mandatory-keyword sep
 *                        mandatory-arg-str stmtend
 *
 *  mandatory-arg-str   = < a string that matches the rule
 *                          mandatory-arg >
 *
 *  mandatory-arg       = true-keyword / false-keyword
 *
 * ANTLR grammar rule
 *  mandatoryStatement : MANDATORY_KEYWORD mandatory STMTEND;
 *  mandatory          : string;
 */

/**
 * Represents listener based call back function corresponding to the "mandatory"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class MandatoryListener {

    /**
     * Creates a new mandatory listener.
     */
    private MandatoryListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (mandatory), performs validation and updates the data model
     * tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processMandatoryEntry(TreeWalkListener listener,
                                          GeneratedYangParser.MandatoryStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, MANDATORY_DATA, "", ENTRY);

        boolean isMandatory = getValidBooleanValue(ctx.mandatory().getText(), MANDATORY_DATA, ctx);

        Parsable tmpNode = listener.getParsedDataStack().peek();
        switch (tmpNode.getYangConstructType()) {
            case LEAF_DATA:
                YangLeaf leaf = (YangLeaf) tmpNode;
                leaf.setMandatory(isMandatory);
                break;
            case CHOICE_DATA: // TODO
                break;
            default:
                throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, MANDATORY_DATA, "", ENTRY));
        }
    }
}