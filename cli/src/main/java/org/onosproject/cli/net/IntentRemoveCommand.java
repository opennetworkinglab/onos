/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.cli.net;

import com.google.common.collect.ImmutableList;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;

/**
 * Removes an intent.
 */
@Command(scope = "onos", name = "remove-intent",
        description = "Removes the specified intent")
public class IntentRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "app",
            description = "Application ID",
            required = false, multiValued = false)
    String applicationIdString = null;

    @Argument(index = 1, name = "key",
            description = "Intent Key",
            required = false, multiValued = false)
    String keyString = null;

    @Option(name = "-p", aliases = "--purge",
            description = "Purge the intent from the store after removal",
            required = false, multiValued = false)
    private boolean purgeAfterRemove = false;

    @Option(name = "-s", aliases = "--sync",
            description = "Waits for the removal before returning",
            required = false, multiValued = false)
    private boolean sync = false;

    private static final EnumSet<IntentState> CAN_PURGE = EnumSet.of(WITHDRAWN, FAILED);

    @Override
    protected void execute() {
        IntentService intentService = get(IntentService.class);
        removeIntent(intentService.getIntents(),
             applicationIdString, keyString,
             purgeAfterRemove, sync);
    }

    /**
     * Purges the intents passed as argument.
     *
     * @param intents list of intents to purge
     */
    private void purgeIntents(Iterable<Intent> intents) {
        IntentService intentService = get(IntentService.class);
        this.purgeAfterRemove = true;
        removeIntentsByAppId(intentService, intents, null);
    }

    /**
     * Purges the intents passed as argument after confirmation is provided
     * for each of them.
     * If no explicit confirmation is provided, the intent is not purged.
     *
     * @param intents list of intents to purge
     */
    public void purgeIntentsInteractive(Iterable<Intent> intents) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        intents.forEach(intent -> {
            System.out.print(String.format("Id=%s, Key=%s, AppId=%s. Remove? [y/N]: ",
                                           intent.id(), intent.key(), intent.appId().name()));
            String response;
            try {
                response = br.readLine();
                response = response.trim().replace("\n", "");
                if ("y".equals(response)) {
                    this.purgeIntents(ImmutableList.of(intent));
                }
            } catch (IOException e) {
                response = "";
            }
            print(response);
        });
    }

    /**
     * Removes the intents passed as argument, assuming these
     * belong to the application's ID provided (if any) and
     * contain a key string.
     *
     * If an application ID is provided, it will be used to further
     * filter the intents to be removed.
     *
     * @param intents list of intents to remove
     * @param applicationIdString application ID to filter intents
     * @param keyString string to filter intents
     * @param purgeAfterRemove states whether the intents should be also purged
     * @param sync states whether the cli should wait for the operation to finish
     *             before returning
     */
    private void removeIntent(Iterable<Intent> intents,
                             String applicationIdString, String keyString,
                             boolean purgeAfterRemove, boolean sync) {
        IntentService intentService = get(IntentService.class);
        CoreService coreService = get(CoreService.class);
        this.applicationIdString = applicationIdString;
        this.keyString = keyString;
        this.purgeAfterRemove = purgeAfterRemove;
        this.sync = sync;
        if (purgeAfterRemove || sync) {
            print("Using \"sync\" to remove/purge intents - this may take a while...");
            print("Check \"summary\" to see remove/purge progress.");
        }

        ApplicationId appId = appId();
        if (!isNullOrEmpty(applicationIdString)) {
            appId = coreService.getAppId(applicationIdString);
            if (appId == null) {
                print("Cannot find application Id %s", applicationIdString);
                return;
            }
        }

        if (isNullOrEmpty(keyString)) {
            removeIntentsByAppId(intentService, intents, appId);

        } else {
            final Key key;
            if (keyString.startsWith("0x")) {
                // The intent uses a LongKey
                keyString = keyString.replaceFirst("0x", "");
                key = Key.of(new BigInteger(keyString, 16).longValue(), appId);
            } else {
                // The intent uses a StringKey
                key = Key.of(keyString, appId);
            }

            Intent intent = intentService.getIntent(key);
            if (intent != null) {
                removeIntent(intentService, intent);
            }
        }
    }

    /**
     * Removes the intents passed as argument.
     *
     * If an application ID is provided, it will be used to further
     * filter the intents to be removed.
     *
     * @param intentService IntentService object
     * @param intents intents to remove
     * @param appId application ID to filter intents
     */
    private void removeIntentsByAppId(IntentService intentService,
                                     Iterable<Intent> intents,
                                     ApplicationId appId) {
        for (Intent intent : intents) {
            if (appId == null || intent.appId().equals(appId)) {
                removeIntent(intentService, intent);
            }
        }
    }

    /**
     * Removes the intent passed as argument.
     *
     * @param intentService IntentService object
     * @param intent intent to remove
     */
    private void removeIntent(IntentService intentService, Intent intent) {
        IntentListener listener = null;
        Key key = intent.key();
        final CountDownLatch withdrawLatch, purgeLatch;
        if (purgeAfterRemove || sync) {
            // set up latch and listener to track uninstall progress
            withdrawLatch = new CountDownLatch(1);
            purgeLatch = purgeAfterRemove ? new CountDownLatch(1) : null;
            listener = (IntentEvent event) -> {
                if (Objects.equals(event.subject().key(), key)) {
                    if (event.type() == IntentEvent.Type.WITHDRAWN ||
                            event.type() == IntentEvent.Type.FAILED) {
                        withdrawLatch.countDown();
                    } else if (purgeAfterRemove &&
                            event.type() == IntentEvent.Type.PURGED) {
                        purgeLatch.countDown();
                    }
                }
            };
            intentService.addListener(listener);
        } else {
            purgeLatch = null;
            withdrawLatch = null;
        }

        // request the withdraw
        intentService.withdraw(intent);

        if (purgeAfterRemove || sync) {
            try { // wait for withdraw event
                withdrawLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                print("Timed out waiting for intent {} withdraw", key);
            }
            if (purgeAfterRemove && CAN_PURGE.contains(intentService.getIntentState(key))) {
                intentService.purge(intent);
                if (sync) { // wait for purge event
                    /* TODO
                       Technically, the event comes before map.remove() is called.
                       If we depend on sync and purge working together, we will
                       need to address this.
                    */
                    try {
                        purgeLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        print("Timed out waiting for intent {} purge", key);
                    }
                }
            }
        }

        if (listener != null) {
            // clean up the listener
            intentService.removeListener(listener);
        }
    }
}
