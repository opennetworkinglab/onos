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
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.parser.Parsable;
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
 * ABNF grammar as per RFC6020
 * meta-stmts          = ;; these stmts can appear in any order
 *                       [organization-stmt stmtsep]
 *                       [contact-stmt stmtsep]
 *                       [description-stmt stmtsep]
 *                       [reference-stmt stmtsep]
 * contact-stmt        = contact-keyword sep string optsep stmtend
 *
 * ANTLR grammar rule
 * meta_stmts : organization_stmt? contact_stmt? description_stmt? reference_stmt?
 *            | organization_stmt? contact_stmt? reference_stmt? description_stmt?
 *            | organization_stmt? description_stmt? contact_stmt? reference_stmt?
 *            | organization_stmt? description_stmt? reference_stmt? contact_stmt?
 *            | organization_stmt? reference_stmt? contact_stmt? description_stmt?
 *            | organization_stmt? reference_stmt? description_stmt? contact_stmt?
 *            | contact_stmt? organization_stmt? description_stmt? reference_stmt?
 *            | contact_stmt? organization_stmt? reference_stmt? description_stmt?
 *            | contact_stmt? reference_stmt? organization_stmt? description_stmt?
 *            | contact_stmt? reference_stmt? description_stmt? organization_stmt?
 *            | contact_stmt? description_stmt? reference_stmt? organization_stmt?
 *            | contact_stmt? description_stmt? organization_stmt? reference_stmt?
 *            | reference_stmt? contact_stmt? organization_stmt? description_stmt?
 *            | reference_stmt? contact_stmt? description_stmt? organization_stmt?
 *            | reference_stmt? organization_stmt? contact_stmt? description_stmt?
 *            | reference_stmt? organization_stmt? description_stmt? contact_stmt?
 *            | reference_stmt? description_stmt? organization_stmt? contact_stmt?
 *            | reference_stmt? description_stmt? contact_stmt? organization_stmt?
 *            | description_stmt? reference_stmt? contact_stmt? organization_stmt?
 *            | description_stmt? reference_stmt? organization_stmt? contact_stmt?
 *            | description_stmt? contact_stmt? reference_stmt? organization_stmt?
 *            | description_stmt? contact_stmt? organization_stmt? reference_stmt?
 *            | description_stmt? organization_stmt? contact_stmt? reference_stmt?
 *            | description_stmt? organization_stmt? reference_stmt? contact_stmt?
 *            ;
 * contact_stmt : CONTACT_KEYWORD string STMTEND;
 */

/**
 * Implements listener based call back function corresponding to the "contact"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class ContactListener {

    /**
     * Creates a new contact listener.
     */
    private ContactListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (contact), perform validations and update the data model
     * tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processContactEntry(TreeWalkListener listener, GeneratedYangParser.ContactStatementContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                                                ParsableDataType.CONTACT_DATA,
                                                String.valueOf(ctx.string().getText()), ListenerErrorLocation.ENTRY);

        // Obtain the node of the stack.
        Parsable tmpNode = listener.getParsedDataStack().peek();
        switch (tmpNode.getParsableDataType()) {
        case MODULE_DATA: {
            YangModule module = (YangModule) tmpNode;
            module.setContact(String.valueOf(ctx.string().getText()));
            break;
        }
        case SUB_MODULE_DATA: {
            YangSubModule subModule = (YangSubModule) tmpNode;
            subModule.setContact(String.valueOf(ctx.string().getText()));
            break;
        }
        default:
            throw new ParserException(
                                      ListenerErrorMessageConstruction
                                              .constructListenerErrorMessage(ListenerErrorType.INVALID_HOLDER,
                                                                             ParsableDataType.CONTACT_DATA,
                                                                             String.valueOf(ctx.string().getText()),
                                                                             ListenerErrorLocation.ENTRY));
        }
    }
}