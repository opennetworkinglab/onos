/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.distributedprimitives.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTree;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;

/**
 * CLI command to manipulate a document tree.
 */
@Command(scope = "onos", name = "document-tree-test",
        description = "Manipulate a document tree")
public class DocumentTreeTestCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name",
            description = "tree name",
            required = true, multiValued = false)
    String name = null;

    @Argument(index = 1, name = "operation",
            description = "operation name",
            required = true, multiValued = false)
    String operation = null;

    @Argument(index = 2, name = "path",
            description = "first arg",
            required = false, multiValued = false)
    String arg1 = null;

    @Argument(index = 3, name = "value",
            description = "second arg",
            required = false, multiValued = false)
    String arg2 = null;

    DocumentTree<String> tree;

    @Override
    protected void doExecute() {
        StorageService storageService = get(StorageService.class);
        tree = storageService.<String>getDocumentTree(name, Serializer.using(KryoNamespaces.BASIC)).asDocumentTree();

        tree.addListener(value -> {
            log.debug("Received event: {}", value);
        });

        if ("set".equals(operation)) {
            log.debug("set {} {}", DocumentPath.from(arg1), arg2);
            print(tree.set(DocumentPath.from(arg1), arg2));
        } else if ("get".equals(operation)) {
            log.debug("get {}", DocumentPath.from(arg1));
            print(tree.get(DocumentPath.from(arg1)));
        } else if ("remove".equals(operation)) {
            log.debug("removeNode {}", DocumentPath.from(arg1));
            print(tree.removeNode(DocumentPath.from(arg1)));
        } else if ("create".equals(operation)) {
            log.debug("create {} {}", DocumentPath.from(arg1), arg2);
            print("%b", tree.create(DocumentPath.from(arg1), arg2));
        } else if ("createRecursive".equals(operation)) {
            log.debug("createRecursive {} {}", DocumentPath.from(arg1), arg2);
            print("%b", tree.createRecursive(DocumentPath.from(arg1), arg2));
        }
    }

    void print(Versioned<String> value) {
        if (value == null) {
            print("null");
        } else {
            print("%s", value.value());
        }
    }
}
