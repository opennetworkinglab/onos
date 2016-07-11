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
 * CLI command to put a value into a transactional map.
 */
@Command(scope = "onos", name = "transactional-map-test-put",
        description = "Put a value into a transactional map")
public class TransactionalMapTestPutCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "numKeys",
            description = "Number of keys to put the value into",
            required = true, multiValued = false)
    private int numKeys = 1;

    @Argument(index = 1, name = "value",
            description = "Value to map with the keys in the map",
            required = true, multiValued = false)
    private String value = null;

    TransactionalMap<String, String> map;
    String prefix = "Key";
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
            for (int i = 1; i <= numKeys; i++) {
                String key = prefix + i;
                String response = map.put(key, value);
                if (response == null) {
                    print("Created Key %s with value %s.", key, value);
                } else {
                    print("Put %s into key %s. The old value was %s.", value, key, response);
                }
            }
            context.commit();
        } catch (Exception e) {
            context.abort();
            throw e;
        }
    }
}
