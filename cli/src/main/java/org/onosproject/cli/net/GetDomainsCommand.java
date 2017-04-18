/*
 * Copyright 2016-present Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.domain.DomainId;
import org.onosproject.net.domain.DomainService;

import java.util.Set;

/**
 * Gets the complete list of domain IDs.
 */
@Command(scope = "onos", name = "domains", description = "Gets the list of domain IDs")
public class GetDomainsCommand extends AbstractShellCommand {

    @Override
    public void execute() {
        DomainService domainService = AbstractShellCommand.get(DomainService.class);

        Set<DomainId> domainIds = domainService.getDomainIds();
        domainIds.forEach(domainId -> print("%s", domainId.id()));
    }
}
