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
package org.onosproject.openstacknetworking.cli;


import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

import java.util.List;
import java.util.SortedSet;

/**
 * OpenStack end point completer.
 */
public class OpenStackEndPointCompleter implements Completer {

    private static final String OPENSTACK_ENDPOINT_EXAMPLE_V2 =
            "http://IP address of OpenStack end point:35357/v2.0 ProjectName ID Password [Perspective]";
    private static final String OPENSTACK_ENDPOINT_EXAMPLE_V3 =
            "http://IP address of OpenStack end point/identity/v3 ProjectName ID Password [Perspective]";
    private static final String SEPARATOR = "\n or \n";

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();

        SortedSet<String> strings = delegate.getStrings();

        strings.add(OPENSTACK_ENDPOINT_EXAMPLE_V2);
        strings.add(SEPARATOR);
        strings.add(OPENSTACK_ENDPOINT_EXAMPLE_V3);

        return delegate.complete(buffer, cursor, candidates);
    }
}
