/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;

/**
 * CLI command to get a value associated with a specific key in a transactional map.
 */
@Command(scope = "onos", name = "transactional-map-test-get",
        description = "Get a value associated with a specific key in a transactional map")
public class TransactionalMapTestGetCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "key",
            description = "Key to get the value of",
            required = true, multiValued = false)
    private String key = null;

    TransactionalMap<String, String> map;
    String mapName = "Test-Map";
    Serializer serializer = Serializer.using(KryoNamespaces.BASIC);

    @Override
    protected void execute() {
        StorageService storageService = get(StorageService.class);
        TransactionContext context;
        context = storageService.transactionContextBuilder().build();
        context.begin();
        try {
            map = context.getTransactionalMap(mapName, serializer);
            String response = map.get(key);
            context.commit();

            if (response == null) {
                print("Key %s not found.", key);
            } else {
                print("Key-value pair (%s, %s) found.", key, response);
            }
        } catch (Exception e) {
            context.abort();
            throw e;
        }
    }
}
