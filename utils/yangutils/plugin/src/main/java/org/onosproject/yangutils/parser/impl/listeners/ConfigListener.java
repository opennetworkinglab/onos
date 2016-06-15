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

import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.CONFIG_DATA;
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
 * config-stmt         = config-keyword sep
 *                       config-arg-str stmtend
 * config-arg-str      = < a string that matches the rule
 *                         config-arg >
 * config-arg          = true-keyword / false-keyword
 *
 * ANTLR grammar rule
 * configStatement : CONFIG_KEYWORD config STMTEND;
 * config          : string;
 */

/**
 * Represents listener based call back function corresponding to the "config"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class ConfigListener {

    /**
     * Creates a new config listener.
     */
    private ConfigListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (config), performs validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processConfigEntry(TreeWalkListener listener,
            GeneratedYangParser.ConfigStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, CONFIG_DATA, "", ENTRY);

        boolean isConfig = getValidBooleanValue(ctx.config().getText(), CONFIG_DATA, ctx);

        Parsable tmpData = listener.getParsedDataStack().peek();
        switch (tmpData.getYangConstructType()) {
            case LEAF_DATA:
                YangLeaf leaf = (YangLeaf) tmpData;
                leaf.setConfig(isConfig);
                break;
            case CONTAINER_DATA:
                YangContainer container = (YangContainer) tmpData;
                container.setConfig(isConfig);
                break;
            case LEAF_LIST_DATA:
                YangLeafList leafList = (YangLeafList) tmpData;
                leafList.setConfig(isConfig);
                break;
            case LIST_DATA:
                YangList yangList = (YangList) tmpData;
                yangList.setConfig(isConfig);
                break;
            case CHOICE_DATA: // TODO
                break;
            default:
                throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, CONFIG_DATA, "", ENTRY));
        }
    }
}
