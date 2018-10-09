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
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;

/**
 * Stops monitoring of an intent by the IMR service.
 */
@Service
@Command(scope = "imr", name = "stopmon",
        description = "Stop monitoring and intent already submitted to the IMR")
public class StopMonitorCommand extends AbstractShellCommand {

    @Option(name = "-l", aliases = "--longkey", description = "Treat intentKey as LongKey",
            required = false, multiValued = false)
    private boolean treatAsLongKey = false;

    @Argument(index = 0, name = "applicationId",
            description = "Application ID that submitted the intent",
            required = true)
    @Completion(ApplicationIdImrCompleter.class)
    private Short appId = null;

    @Argument(index = 1, name = "applicationName",
            description = "Application Name that submitted the intent",
            required = true)
    @Completion(ApplicationNameImrCompleter.class)
    private String appName = null;

    @Argument(index = 2, name = "intentKey",
            description = "String representation of the key of the intent",
            required = false)
    @Completion(IntentKeyImrCompleter.class)
    private String key = null;

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
                        if (imrService.stopMonitorIntent(intentKeyLong)) {
                            print("Stopped monitoring of intent with LongKey %s", intentKeyLong);
                            return;
                        }
                    } catch (NumberFormatException nfe) {
                        print("\"%s\" is not a valid LongKey", key);
                        return;
                    }
                } else {
                    Key intentKeyString = Key.of(key, new DefaultApplicationId(appId, appName));
                    if (imrService.stopMonitorIntent(intentKeyString)) {
                        print("Stopped monitoring of intent with StringKey %s", intentKeyString);
                        return;
                    }
                }

                //No intent found in IMR
                print("No monitored intent with key %s found", key);
            } else {
                intentService.getIntents().forEach(i -> {
                    if (i.appId().equals(new DefaultApplicationId(appId, appName))) {
                        imrService.stopMonitorIntent(i.key());
                        print("Stopped monitoring of intent with key %s", i.key());
                    }
                });
            }
        }

    }
}
