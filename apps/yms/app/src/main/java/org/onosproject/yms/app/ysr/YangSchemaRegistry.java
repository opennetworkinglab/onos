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

package org.onosproject.yms.app.ysr;

import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yms.ysr.YangModuleIdentifier;
import org.onosproject.yms.ysr.YangModuleLibrary;

/**
 * Abstraction of entity which provides interfaces to YANG schema registry.
 */
public interface YangSchemaRegistry {

    /**
     * Registers applications to YMS.
     *
     * @param managerObject application's object
     * @param serviceClass  service class which needs to be
     *                      registered
     */
    void registerApplication(Object managerObject, Class<?> serviceClass);

    /**
     * Unregisters applications to YMS.
     *
     * @param managerObject application's object
     * @param serviceClass  service class which needs to be unregistered
     */
    void unRegisterApplication(Object managerObject, Class<?> serviceClass);

    /**
     * Returns application's implementation's class object.
     *
     * @param yangSchemaNode application's schema node
     * @return application's implementation's class object
     */
    Object getRegisteredApplication(YangSchemaNode yangSchemaNode);

    /**
     * Returns YANG schema node using schema name.
     *
     * @param schemaName module name.
     * @return YANG schema node using schema name
     */
    YangSchemaNode getYangSchemaNodeUsingSchemaName(String schemaName);

    /**
     * Returns YANG schema nodes using application name.
     *
     * @param appName application's service name
     * @return YANG schema nodes using application name
     */
    YangSchemaNode getYangSchemaNodeUsingAppName(String appName);

    /**
     * Returns YANG schema nodes using root interface file name.
     *
     * @param rootInterfaceFileName name of generated interface file
     *                              for root node
     * @return YANG schema nodes using root interface file name
     */
    YangSchemaNode
    getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
            String rootInterfaceFileName);

    /**
     * Returns YANG schema nodes using root op param file name.
     *
     * @param rootOpParamFileName name of generated op param file for root node
     * @return YANG schema nodes using root op param file name
     */
    YangSchemaNode
    getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
            String rootOpParamFileName);

    /**
     * Returns YANG schema node of root for notifications.
     *
     * @param eventSubject event subject
     * @return YANG schema node of root for notifications
     */
    YangSchemaNode getRootYangSchemaNodeForNotification(String eventSubject);

    /**
     * Returns registered service class.
     *
     * @param schemaNode YANG schema node
     * @return registered service class
     */
    Class<?> getRegisteredClass(YangSchemaNode schemaNode);

    /**
     * Verifies if the manager object is already registered with notification
     * handler.
     *
     * @param appObj  application object
     * @param service service class
     * @return true if the manager object is already registered with
     * notification handler
     */
    boolean verifyNotificationObject(Object appObj, Class<?> service);

    /**
     * Clears database for YSR.
     */
    void flushYsrData();

    /**
     * Protocols like RESTCONF, use the definitions within the YANG modules
     * advertised by the server are used to construct an RPC operation or
     * data resource identifier.
     * <p>
     * Schema Resource:
     * The server can optionally support retrieval of the YANG modules it
     * supports.
     *
     * @param moduleIdentifier module's identifier
     * @return YANG file contents of the requested YANG module.
     */
    String getYangFile(YangModuleIdentifier moduleIdentifier);

    /**
     * Process module library for a registered service.
     *
     * @param serviceName service class name
     * @param library     YANG module library
     */
    void processModuleLibrary(String serviceName, YangModuleLibrary library);

    /**
     * Returns YANG schema node for a given namespace while xml decoding.
     * <p>
     * According to rfc 6020 Xml should not have module name in it but when
     * decoder wants to convert xml to YANG object it will need module schema
     * which it can get only by using module name from YSR. So if YCH sends
     * namespace of a module we can given it the schema node of module. In
     * this case namespace should be unique.
     * </p>
     *
     * @param nameSpace name space of module
     * @return module schema node
     */
    YangSchemaNode getSchemaWrtNameSpace(String nameSpace);

}
