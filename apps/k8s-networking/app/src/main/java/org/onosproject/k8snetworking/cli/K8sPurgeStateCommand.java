/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snetworking.api.K8sEndpointsAdminService;
import org.onosproject.k8snetworking.api.K8sNetworkAdminService;
import org.onosproject.k8snetworking.api.K8sPodAdminService;
import org.onosproject.k8snetworking.api.K8sServiceAdminService;

/**
 * Purges all existing kubernetes states.
 */
@Service
@Command(scope = "onos", name = "k8s-purge-states",
        description = "Purges all kubernetes states")
public class K8sPurgeStateCommand extends AbstractShellCommand {
    @Override
    protected void doExecute() {
        get(K8sPodAdminService.class).clear();
        get(K8sNetworkAdminService.class).clear();
        get(K8sEndpointsAdminService.class).clear();
        get(K8sServiceAdminService.class).clear();
    }
}
