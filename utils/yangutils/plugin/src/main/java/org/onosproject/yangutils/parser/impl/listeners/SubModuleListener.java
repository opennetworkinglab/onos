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

import org.onosproject.yangutils.datamodel.ResolvableType;
import org.onosproject.yangutils.datamodel.YangReferenceResolver;
import org.onosproject.yangutils.datamodel.YangRevision;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.linker.exceptions.LinkerException;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.GeneratedLanguage.JAVA_GENERATION;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.SUB_MODULE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.setCurrentDateForRevision;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsEmpty;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.translator.tojava.YangDataModelFactory.getYangSubModuleNode;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * submodule-stmt      = optsep submodule-keyword sep identifier-arg-str
 *                             optsep
 *                             "{" stmtsep
 *                                 submodule-header-stmts
 *                                 linkage-stmts
 *                                 meta-stmts
 *                                 revision-stmts
 *                                 body-stmts
 *                             "}" optsep
 *
 * ANTLR grammar rule
 * submodule_stmt : SUBMODULE_KEYWORD identifier LEFT_CURLY_BRACE submodule_body* RIGHT_CURLY_BRACE;
 * submodule_body : submodule_header_statement linkage_stmts meta_stmts revision_stmts body_stmts;
 */

/**
 * Represents listener based call back function corresponding to the "submodule"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class SubModuleListener {

    /**
     * Creates a new sub module listener.
     */
    private SubModuleListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule (sub
     * module), perform validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processSubModuleEntry(TreeWalkListener listener,
                                             GeneratedYangParser.SubModuleStatementContext ctx) {

        // Check if stack is empty.
        checkStackIsEmpty(listener, INVALID_HOLDER, SUB_MODULE_DATA, ctx.identifier().getText(),
                ENTRY);

        String identifier = getValidIdentifier(ctx.identifier().getText(), SUB_MODULE_DATA, ctx);

        YangSubModule yangSubModule = getYangSubModuleNode(JAVA_GENERATION);
        yangSubModule.setName(identifier);

        if (ctx.submoduleBody().submoduleHeaderStatement().yangVersionStatement() == null) {
            yangSubModule.setVersion((byte) 1);
        }

        if (ctx.submoduleBody().revisionStatements().revisionStatement().isEmpty()) {
            String currentDate = setCurrentDateForRevision();
            YangRevision currentRevision = new YangRevision();
            currentRevision.setRevDate(currentDate);
            yangSubModule.setRevision(currentRevision);
        }

        listener.getParsedDataStack().push(yangSubModule);
    }

    /**
     * It is called when parser exits from grammar rule (submodule), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processSubModuleExit(TreeWalkListener listener,
                                            GeneratedYangParser.SubModuleStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, SUB_MODULE_DATA, ctx.identifier().getText(),
                EXIT);

        if (!(listener.getParsedDataStack().peek() instanceof YangSubModule)) {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, SUB_MODULE_DATA,
                    ctx.identifier().getText(), EXIT));
        }
        try {
            ((YangReferenceResolver) listener.getParsedDataStack().peek())
                    .resolveSelfFileLinking(ResolvableType.YANG_USES);
            ((YangReferenceResolver) listener.getParsedDataStack().peek())
                    .resolveSelfFileLinking(ResolvableType.YANG_DERIVED_DATA_TYPE);
        } catch (DataModelException e) {
            LinkerException linkerException = new LinkerException(e.getMessage());
            linkerException.setLine(e.getLineNumber());
            linkerException.setCharPosition(e.getCharPositionInLine());
            throw linkerException;
        }
    }
}
