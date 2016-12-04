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

package org.onosproject.yms.app.ytb;

import org.onosproject.yms.app.ysr.TestYangSchemaNodeProvider;

/**
 * Represents the abstract class for ytb test classes having common methods
 * and the constants.
 */
public abstract class YtbErrMsgAndConstants {

    /**
     * Static attribute of root name.
     */
    public static final String ROOT_NAME = "rootName";

    /**
     * Static attribute of root name space.
     */
    public static final String ROOT_NAME_SPACE = "rootNameSpace";

    /**
     * Static attribute of module which is YANG name.
     */
    public static final String MODULE = "module";

    /**
     * Static attribute of list which is YANG name.
     */
    public static final String LIST = "list";

    /**
     * Static attribute of leaf which is YANG name.
     */
    public static final String LEAF = "leaf";

    /**
     * Static attribute of leaf-list which is YANG name.
     */
    public static final String LEAF_LIST = "leaf-list";

    /**
     * Static attribute of container which is YANG name.
     */
    public static final String CONTAINER = "container";

    /**
     * Static attribute of name predict.
     */
    public static final String PREDICT = "predict";

    /**
     * Static attribute of name catch.
     */
    public static final String CATCH = "catch";

    /**
     * Static attribute of YANG file name.
     */
    public static final String RPC_NAME = "YtbSimpleRpcResponse";

    /**
     * Static attribute of name rpc.
     */
    public static final String RPC = "rpc";
    public static final String HUNDRED = "hundred";
    /**
     * Created a schema node provider, which will register the app.
     */
    public TestYangSchemaNodeProvider schemaProvider =
            new TestYangSchemaNodeProvider();

    /**
     * Returns the error message for when leaf value doesn't match with the
     * expected value. It takes name of leaf and expected value as its
     * parameter, to throw the message.
     *
     * @param name  leaf name
     * @param value expected value of leaf
     * @return error message of leaf value as incorrect
     */
    public static String getInCrtLeafValue(String name, String value) {
        return "The value of leaf " + name + " is not " + value;
    }

    /**
     * Returns the error message, when node name doesn't match with the
     * expected value. It takes YANG name of the node and the node name as
     * parameter, to throw the message.
     *
     * @param node     YANG node name
     * @param nodeName node name
     * @return error message as the node name is incorrect
     */
    public static String getInCrtName(String node, String nodeName) {
        return getCapitalCase(node) + "'s name " + nodeName + " is incorrect.";
    }

    /**
     * Returns the error message, when operation type doesn't match with the
     * expected value. It takes YANG name of the node and the node name as
     * parameter, to throw the message.
     *
     * @param node     YANG node name
     * @param nodeName node name
     * @return error message as the operation type is incorrect
     */
    public static String getInCrtOpType(String node, String nodeName) {
        return "The operation type of " + node + " " + nodeName + " is " +
                "incorrect";
    }

    /**
     * Returns the error message for when leaf-list value doesn't match with the
     * expected value. It takes name of leaf-list and expected value as its
     * parameter, to throw the message.
     *
     * @param name  leaf-list name
     * @param value value in leaf-list
     * @return error message as the value in the leaf-list is incorrect
     */
    public static String getInCrtLeafListValue(String name, String value) {
        return "The leaf-list " + name + " does not have " + value + " in it.";
    }

    /**
     * Returns the capital cased first letter of the given string.
     *
     * @param name string to be capital cased
     * @return capital cased string
     */
    private static String getCapitalCase(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
