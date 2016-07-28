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

import org.onosproject.yangutils.datamodel.YangCompilerAnnotation;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.COMPILER_ANNOTATION_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidPrefix;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.removeQuotesAndHandleConcat;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *   compiler-annotation-stmt = prefix:compiler-annotation-keyword string
 *                          "{"
 *                              [app-data-structure-stmt stmtsep]
 *                              [app-extended-stmt stmtsep]
 *                          "}"
 *
 * ANTLR grammar rule
 *   compilerAnnotationStatement : COMPILER_ANNOTATION string LEFT_CURLY_BRACE
 *        compilerAnnotationBodyStatement RIGHT_CURLY_BRACE;
 *
 *   compilerAnnotationBodyStatement : appDataStructureStatement? appExtendedStatement? ;
 */

/**
 * Represents listener based call back function corresponding to the "compiler-annotation"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class CompilerAnnotationListener {

    /**
     * Creates a new compiler-annotation listener.
     */
    private CompilerAnnotationListener() {
    }

    /**
     * Performs validation and updates the data model tree. It is called when parser receives an
     * input matching the grammar rule(compiler-annotation).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processCompilerAnnotationEntry(TreeWalkListener listener,
                                                      GeneratedYangParser.CompilerAnnotationStatementContext ctx) {
        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, COMPILER_ANNOTATION_DATA, ctx.string().getText(), ENTRY);
        String prefix = getValidPrefix(ctx.COMPILER_ANNOTATION().getText(), COMPILER_ANNOTATION_DATA, ctx);

        YangCompilerAnnotation compilerAnnotation = new YangCompilerAnnotation();
        compilerAnnotation.setPrefix(prefix);
        compilerAnnotation.setPath(removeQuotesAndHandleConcat(ctx.string().getText()));

        Parsable curData = listener.getParsedDataStack().peek();
        switch (curData.getYangConstructType()) {
            case MODULE_DATA:
                YangModule module = ((YangModule) curData);
                module.addCompilerAnnotation(compilerAnnotation);
                break;
            case SUB_MODULE_DATA:
                YangSubModule subModule = ((YangSubModule) curData);
                subModule.addCompilerAnnotation(compilerAnnotation);
                break;
            default:
                throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, COMPILER_ANNOTATION_DATA,
                        ctx.string().getText(), ENTRY));
        }
        listener.getParsedDataStack().push(compilerAnnotation);
    }

    /**
     * Performs validation and updates the data model tree. It is called when parser
     * exits from grammar rule (compiler-annotation).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processCompilerAnnotationExit(TreeWalkListener listener,
                                                     GeneratedYangParser.CompilerAnnotationStatementContext ctx) {

        checkStackIsNotEmpty(listener, MISSING_HOLDER, COMPILER_ANNOTATION_DATA, ctx.string().getText(), EXIT);
        if (!(listener.getParsedDataStack().peek() instanceof YangCompilerAnnotation)) {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, COMPILER_ANNOTATION_DATA,
                    ctx.string().getText(), EXIT));
        }
        listener.getParsedDataStack().pop();
    }
}
