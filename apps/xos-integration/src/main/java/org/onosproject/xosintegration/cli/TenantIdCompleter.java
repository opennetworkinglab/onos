/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.xosintegration.cli;

import java.util.List;

import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.xosintegration.VoltTenant;
import org.onosproject.xosintegration.VoltTenantService;
import static java.util.stream.Collectors.toList;

import static org.onosproject.cli.AbstractShellCommand.get;


/**
 * Application command completer.
 */
public class TenantIdCompleter extends AbstractChoicesCompleter {
    @Override
    public List<String> choices() {
        VoltTenantService service = get(VoltTenantService.class);

        return service.getAllTenants().stream()
                .map(VoltTenant::id)
                .map(Object::toString)
                .collect(toList());

    }

}
