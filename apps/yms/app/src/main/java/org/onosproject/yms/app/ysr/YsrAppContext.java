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

import com.google.common.collect.ImmutableMap;
import org.onosproject.yangutils.datamodel.YangSchemaNode;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents registered application's context for YANG schema registry.
 */
public class YsrAppContext {

    /**
     * Current application's YANG schema node.
     */
    private YangSchemaNode curNode;

    /**
     * Current application's YANG schema node with different revision store.
     */
    private final ConcurrentMap<String, YangSchemaNode>
            multiRevisionSchemaNodeStore;

    /**
     * Current application's object.
     */
    private Object appObject;

    /**
     * Jar file path.
     */
    private String jarPath;

    /**
     * If for current object notification is registered.
     */
    private boolean isNotificationRegistered;

    /**
     * Creates an instance of YANG schema registry application context.
     */
    YsrAppContext() {
        multiRevisionSchemaNodeStore = new ConcurrentHashMap<>();
    }

    /**
     * Returns current application's object.
     *
     * @return current application's object
     */
    Object appObject() {
        return appObject;
    }

    /**
     * Sets current application's object.
     *
     * @param appObject current application's object
     */
    void appObject(Object appObject) {
        this.appObject = appObject;
    }

    /**
     * Returns current application's YANG schema node.
     *
     * @return current application's YANG schema node
     */
    YangSchemaNode curNode() {
        return curNode;
    }

    /**
     * Sets current application's schema node.
     *
     * @param node current schema's node
     */
    void curNode(YangSchemaNode node) {
        curNode = node;
    }

    /**
     * Returns jar file path.
     *
     * @return jar file path
     */
    String jarPath() {
        return jarPath;
    }

    /**
     * Sets jar file path.
     *
     * @param jarPath jar file path
     */
    void jarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    @Override
    public int hashCode() {
        return Objects.hash(curNode, appObject);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof YsrAppContext) {
            YsrAppContext that = (YsrAppContext) obj;
            return Objects.equals(curNode, that.curNode) &&
                    Objects.equals(appObject, that.appObject);
        }
        return false;
    }

    /**
     * Returns true if for application object notification is registered.
     *
     * @return true if for application object notification is registered
     */
    boolean isNotificationRegistered() {
        return isNotificationRegistered;
    }

    /**
     * Sets true if for application object notification is registered.
     *
     * @param notificationRegistered true if for application object notification is registered
     */
    void setNotificationRegistered(boolean notificationRegistered) {
        isNotificationRegistered = notificationRegistered;
    }

    /**
     * Returns YANG schema node store for specific revision.
     *
     * @return YANG schema node store for specific revision
     */
    Map<String, YangSchemaNode> getYangSchemaNodeForRevisionStore() {
        return ImmutableMap.copyOf(multiRevisionSchemaNodeStore);
    }

    /**
     * Returns a schema node for specific revision from store.
     *
     * @param nodeNameWithRevision schema node name for specific revision
     * @return schema node for specific revision.
     */
    YangSchemaNode getSchemaNodeForRevisionStore(String nodeNameWithRevision) {
        return multiRevisionSchemaNodeStore.get(nodeNameWithRevision);
    }

    /**
     * Removes a schema node of specific revision from store.
     *
     * @param nodeNameWithRevision schema node name for specific revision
     */
    void removeSchemaNodeForRevisionStore(String nodeNameWithRevision) {
        multiRevisionSchemaNodeStore.remove(nodeNameWithRevision);
    }

    /**
     * Adds schema node with revision from store.
     *
     * @param nodeNameWithRevision schema node name for specific revision
     * @param schemaNode           schema node for specific revision
     */
    void addSchemaNodeWithRevisionStore(String nodeNameWithRevision, YangSchemaNode schemaNode) {
        multiRevisionSchemaNodeStore.put(nodeNameWithRevision, schemaNode);
    }
}
