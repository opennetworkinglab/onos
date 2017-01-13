/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.cli.net.completer;

import java.util.List;
import java.util.stream.Collectors;

import org.onlab.util.Tools;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentService;

/**
 * IntentId Completer.
 */
public class IntentIdCompleter extends AbstractChoicesCompleter {

    @Override
    protected List<String> choices() {
        IntentService service = AbstractShellCommand.get(IntentService.class);

        return Tools.stream(service.getIntents())
            .map(Intent::id)
            .map(IntentId::toString)
            .collect(Collectors.toList());
    }

}
