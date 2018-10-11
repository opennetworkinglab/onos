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
package org.onosproject.cli.net;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onlab.util.Tools;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.completer.IntentIdCompleter;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentService;

/**
 * Displays details about an Intent in the system.
 */
@Service
@Command(scope = "onos", name = "intent-details",
         description = "Displays intent details")
public class IntentDetailsCommand extends AbstractShellCommand {

    @Option(name = "--id",
            description = "Filter intent by specific Id", multiValued = true)
    @Completion(IntentIdCompleter.class)
    private List<String> idsStr;

    private Set<IntentId> ids = null;


    @Override
    protected void doExecute() {
        detailIntents(idsStr);
    }

    /**
     * Print detailed data for intents, given a list of IDs.
     *
     * @param intentsIds List of intent IDs
     */
    public void detailIntents(List<String> intentsIds) {
        if (intentsIds != null) {
            ids = intentsIds.stream()
                    .map(IntentId::valueOf)
                    .collect(Collectors.toSet());
        }

        IntentService service = get(IntentService.class);

        Tools.stream(service.getIntentData())
                .filter(this::filter)
                .forEach(this::printIntentData);
    }

    private boolean filter(IntentData data) {
        if (ids != null && !ids.contains(data.intent().id())) {
            return false;
        }

        return true;
    }

    private void printIntentData(IntentData data) {
        print("Key: %s ID: %s", data.key(), data.intent().id());

        print(" Request: %s Current: %s", data.request(), data.state());

        print(" intent: %s", s(data.intent()));

        data.installables().stream()
            .forEach(this::printInstallable);

        // empty line
        print("");
    }

    private void printInstallable(Intent installable) {
        print(" installable: %s %s", installable.getClass().getSimpleName(),
                                     installable.id());

        print("  resources: %s", installable.resources().stream()
                                    .filter(r -> !(r instanceof Link))
                                    .map(this::s)
                                    .collect(Collectors.joining(", ")));

        print("  links: %s", installable.resources().stream()
                              .filter(Link.class::isInstance)
                              .map(Link.class::cast)
                              .map(LinkKey::linkKey)
                              .map(l -> String.format("%s -> %s", l.src(), l.dst()))
                              .collect(Collectors.joining(", ")));
    }

    protected String s(Object o) {
        return simplify(String.valueOf(o));
    }

    /**
     * Simplify toString result for CLI.
     *
     * @param input String
     * @return simplified String
     */
    public static String simplify(String input) {
        String after = input
                // omit redundant info
                .replaceAll("treatment=DefaultTrafficTreatment", "treatment=")
                .replaceAll("selector=DefaultTrafficSelector", "selector=")
                // shorten AppId
                .replaceAll("DefaultApplicationId\\{id=(\\d+), name=([.\\w]+)\\}", "$2($1)")
                // omit empty list/array attribute
                .replaceAll("(, )?\\w+=\\[\\]", "")
                // omit empty map attribute
                .replaceAll("(, )?\\w+=\\{\\}", "")
                // omit Object which became empty
                .replaceAll("(, )?\\w+\\{\\}", "\\{\\}")
                // shorten FilteredConnectPoint
                .replaceAll("FilteredConnectPoint", "")
                // trim prefix Default
                .replaceAll("Default(\\w+)\\{", "$1\\{")
                .replaceAll(", , ", ", ");

        return after.equals(input) ? input : simplify(after);
    }

}
