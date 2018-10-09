/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.imr.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.imr.IntentMonitorAndRerouteService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;

/**
 * Starts monitoring of an intent submitting its key to the IMR service.
 */
@Service
@Command(scope = "imr", name = "startmon",
        description = "Submit an intent to the IMR application to start monitoring")
public class StartMonitorCommand extends AbstractShellCommand {

    @Option(name = "-l", aliases = "--longkey", description = "Treat intentKey as LongKey",
            required = false, multiValued = false)
    private boolean treatAsLongKey = false;

    @Argument(index = 0, name = "applicationId",
            description = "Application ID that submitted the intent",
            required = true)
    @Completion(ApplicationIdImrCompleter.class)
    private Short appId;

    @Argument(index = 1, name = "applicationName",
            description = "Application Name that submitted the intent",
            required = true)
    @Completion(ApplicationNameImrCompleter.class)
    private String appName;

    @Argument(index = 2, name = "intentKey",
            description = "String representation of the key of the intent",
            required = false)
    @Completion(IntentKeyImrCompleter.class)
    private String key;

    private IntentMonitorAndRerouteService imrService;
    private IntentService intentService;

    @Override
    protected void doExecute() {
        imrService = get(IntentMonitorAndRerouteService.class);
        intentService = get(IntentService.class);

        if (appId != null && appName != null) {
            if (key != null) {
                /*
                Intent key might be a StringKey or a LongKey, but in any case is
                provided via CLI as a string. To solve only ambiguity we check if
                "--longkey" CLI parameter has been set.
                 */
                if (treatAsLongKey) {
                    try {
                        Key intentKeyLong = Key.of(Integer.decode(key), new DefaultApplicationId(appId, appName));
                        for (Intent intent : intentService.getIntents()) {
                            if (intent.key().equals(intentKeyLong)) {
                                imrService.startMonitorIntent(intentKeyLong);
                                print("Started monitoring of intent with LongKey %s", intentKeyLong);
                                return;
                            }
                        }
                        imrService.startMonitorIntent(intentKeyLong);
                        print("Started monitoring of intent with LongKey %s, even if not yet submitted", intentKeyLong);
                    } catch (NumberFormatException nfe) {
                        print("\"%s\" is not a valid LongKey", key);
                    }
                } else {
                    Key intentKeyString = Key.of(key, new DefaultApplicationId(appId, appName));
                    for (Intent intent : intentService.getIntents()) {
                        if (intent.key().equals(intentKeyString)) {
                            imrService.startMonitorIntent(intentKeyString);
                            print("Started monitoring of intent with StringKey %s", intentKeyString);
                            return;
                        }
                    }
                    imrService.startMonitorIntent(intentKeyString);
                    print("Started monitoring of intent with StringKey %s, even if not yet submitted", intentKeyString);
                }
            } else {
                intentService.getIntents().forEach(i -> {
                    if (i.appId().equals(new DefaultApplicationId(appId, appName))) {
                        imrService.startMonitorIntent(i.key());
                        print("Started monitoring of intent with Key %s", i.key());
                    }
                });
            }
        }
    }
}
