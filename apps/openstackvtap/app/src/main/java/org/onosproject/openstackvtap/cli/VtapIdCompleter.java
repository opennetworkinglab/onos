/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.cli;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapService;

import java.util.List;
import java.util.SortedSet;

import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getVtapTypeFromString;

/**
 * vTap ID completer.
 */
public class VtapIdCompleter implements Completer {

    private static final String VTAP_TYPE = "none";

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {

        OpenstackVtap.Type type = getVtapTypeFromString(VTAP_TYPE);

        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();
        SortedSet<String> strings = delegate.getStrings();

        OpenstackVtapService service = AbstractShellCommand.get(OpenstackVtapService.class);

        service.getVtaps(type).forEach(t -> {
            strings.add(t.id().toString());
        });

        return delegate.complete(buffer, cursor, candidates);
    }
}
