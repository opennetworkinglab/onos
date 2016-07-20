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

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangRpc;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.GeneratedLanguage.JAVA_GENERATION;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.DESCRIPTION_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.GROUPING_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.INPUT_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.OUTPUT_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.REFERENCE_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.RPC_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.STATUS_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.TYPEDEF_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerCollisionDetector.detectCollidingChildUtil;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.*;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.validateCardinalityMaxOne;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.validateMutuallyExclusiveChilds;
import static org.onosproject.yangutils.translator.tojava.YangDataModelFactory.getYangRpcNode;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *  rpc-stmt            = rpc-keyword sep identifier-arg-str optsep
 *                        (";" /
 *                         "{" stmtsep
 *                             ;; these stmts can appear in any order
 *                             *(if-feature-stmt stmtsep)
 *                             [status-stmt stmtsep]
 *                             [description-stmt stmtsep]
 *                             [reference-stmt stmtsep]
 *                             *((typedef-stmt /
 *                                grouping-stmt) stmtsep)
 *                             [input-stmt stmtsep]
 *                             [output-stmt stmtsep]
 *                         "}")
 *
 * ANTLR grammar rule
 *  rpcStatement : RPC_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE (ifFeatureStatement | statusStatement
 *               | descriptionStatement | referenceStatement | typedefStatement | groupingStatement | inputStatement
 *               | outputStatement)* RIGHT_CURLY_BRACE);
 */

/**
 * Represents listener based call back function corresponding to the "rpc"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class RpcListener {

    /**
     * Creates a new rpc listener.
     */
    private RpcListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (rpc), performs validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processRpcEntry(TreeWalkListener listener,
                                             GeneratedYangParser.RpcStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, RPC_DATA, ctx.identifier().getText(), ENTRY);

        String identifier = getValidIdentifier(ctx.identifier().getText(), RPC_DATA, ctx);

        // Validate sub statement cardinality.
        validateSubStatementsCardinality(ctx);

        // Check for identifier collision
        int line = ctx.getStart().getLine();
        int charPositionInLine = ctx.getStart().getCharPositionInLine();
        detectCollidingChildUtil(listener, line, charPositionInLine, identifier, RPC_DATA);

        Parsable curData = listener.getParsedDataStack().peek();
        if (curData instanceof YangModule || curData instanceof YangSubModule) {

            YangNode curNode = (YangNode) curData;
            YangRpc yangRpc = getYangRpcNode(JAVA_GENERATION);
            yangRpc.setName(identifier);
            try {
                curNode.addChild(yangRpc);
            } catch (DataModelException e) {
                throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                        RPC_DATA, ctx.identifier().getText(), ENTRY, e.getMessage()));
            }
            listener.getParsedDataStack().push(yangRpc);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, RPC_DATA,
                    ctx.identifier().getText(), ENTRY));
        }
    }

    /**
     * It is called when parser exits from grammar rule (rpc), it perform
     * validations and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processRpcExit(TreeWalkListener listener,
                                            GeneratedYangParser.RpcStatementContext ctx) {

         //Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, RPC_DATA, ctx.identifier().getText(), EXIT);

        if (!(listener.getParsedDataStack().peek() instanceof YangRpc)) {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, RPC_DATA,
                    ctx.identifier().getText(), EXIT));
        }
        listener.getParsedDataStack().pop();
    }

    /**
     * Validates the cardinality of rpc sub-statements as per grammar.
     *
     * @param ctx context object of the grammar rule
     */
    private static void validateSubStatementsCardinality(GeneratedYangParser.RpcStatementContext ctx) {

        validateCardinalityMaxOne(ctx.statusStatement(), STATUS_DATA, RPC_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.descriptionStatement(), DESCRIPTION_DATA, RPC_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.referenceStatement(), REFERENCE_DATA, RPC_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.inputStatement(), INPUT_DATA, RPC_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.outputStatement(), OUTPUT_DATA, RPC_DATA, ctx.identifier().getText());
        validateMutuallyExclusiveChilds(ctx.typedefStatement(), TYPEDEF_DATA, ctx.groupingStatement(), GROUPING_DATA,
                RPC_DATA, ctx.identifier().getText());
    }

}
