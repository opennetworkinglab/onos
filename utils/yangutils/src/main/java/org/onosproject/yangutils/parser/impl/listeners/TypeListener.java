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

import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangDerivedType;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.utils.YangConstructType.TYPE_DATA;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *  type-stmt           = type-keyword sep identifier-ref-arg-str optsep
 *                        (";" /
 *                         "{" stmtsep
 *                            type-body-stmts
 *                         "}")
 *
 * ANTLR grammar rule
 * typeStatement : TYPE_KEYWORD string (STMTEND | LEFT_CURLY_BRACE typeBodyStatements RIGHT_CURLY_BRACE);
 */

/**
 * Implements listener based call back function corresponding to the "type" rule
 * defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class TypeListener {

    /**
     * Creates a new type listener.
     */
    private TypeListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (type), performs validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processTypeEntry(TreeWalkListener listener,
            GeneratedYangParser.TypeStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, TYPE_DATA, ctx.string().getText(), ENTRY);

        YangDataTypes yangDataTypes = YangDataTypes.getType(ctx.string().getText());
        YangType<?> type = new YangType();

        type.setDataTypeName(ctx.string().getText());
        type.setDataType(yangDataTypes);

        listener.getParsedDataStack().push(type);
    }

    /**
     * It is called when parser exits from grammar rule (type), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processTypeExit(TreeWalkListener listener,
            GeneratedYangParser.TypeStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_CURRENT_HOLDER, TYPE_DATA, ctx.string().getText(), EXIT);

        Parsable type = listener.getParsedDataStack().pop();
        if (!(type instanceof YangType)) {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, TYPE_DATA,
                    ctx.string().getText(), EXIT));
        }

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, TYPE_DATA, ctx.string().getText(), EXIT);

        Parsable tmpData = listener.getParsedDataStack().peek();
        switch (tmpData.getYangConstructType()) {
            case LEAF_DATA:
                YangLeaf leaf = (YangLeaf) tmpData;
                leaf.setDataType((YangType<?>) type);
                break;
            case LEAF_LIST_DATA:
                YangLeafList leafList = (YangLeafList) tmpData;
                leafList.setDataType((YangType<?>) type);
                break;
            case TYPEDEF_DATA:

                /* Prepare the base type info and set in derived type */
                YangTypeDef typeDef = (YangTypeDef) tmpData;
                YangType<YangDerivedType> derivedType = typeDef.getDerivedType();
                if (derivedType == null) {
                    //TODO: set the error info correctly, to depict missing info
                    throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, TYPE_DATA,
                            ctx.string().getText(), ENTRY));
                }

                YangDerivedType derivedTypeInfo = new YangDerivedType();
                if (((YangType<?>) type).getDataType() != YangDataTypes.DERIVED) {
                    derivedTypeInfo.setEffectiveYangBuiltInType(((YangType<?>) type).getDataType());
                } else {
                    /*
                     * It will be resolved in the validate data model at exit.
                     * Nothing needs to be done.
                     */
                }
                derivedTypeInfo.setBaseType((YangType<?>) type);
                derivedType.setDataTypeExtendedInfo(derivedTypeInfo);

                break;
            //TODO: union, deviate replacement statement.case TYPEDEF_DATA: //TODO

            default:
                throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, TYPE_DATA,
                        ctx.string().getText(), EXIT));
        }
    }
}
