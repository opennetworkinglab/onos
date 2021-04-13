/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerService;

import java.util.Comparator;
import java.util.List;

import static org.onosproject.kubevirtnetworking.api.Constants.CLI_IP_ADDRESSES_LENGTH;
import static org.onosproject.kubevirtnetworking.api.Constants.CLI_IP_ADDRESS_LENGTH;
import static org.onosproject.kubevirtnetworking.api.Constants.CLI_MARGIN_LENGTH;
import static org.onosproject.kubevirtnetworking.api.Constants.CLI_NAME_LENGTH;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.genFormatString;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.prettyJson;

/**
 * Lists kubevirt load balancers.
 */
@Service
@Command(scope = "onos", name = "kubevirt-lbs",
        description = "Lists all kubevirt load balancers")
public class KubevirtListLoadBalancerCommand extends AbstractShellCommand {
    @Override
    protected void doExecute() throws Exception {
        KubevirtLoadBalancerService service = get(KubevirtLoadBalancerService.class);
        List<KubevirtLoadBalancer> lbs = Lists.newArrayList(service.loadBalancers());
        lbs.sort(Comparator.comparing(KubevirtLoadBalancer::name));

        String format = genFormatString(ImmutableList.of(CLI_NAME_LENGTH, CLI_NAME_LENGTH,
                CLI_IP_ADDRESS_LENGTH, CLI_IP_ADDRESSES_LENGTH));

        if (outputJson()) {
            print("%s", json(lbs));
        } else {
            print(format, "Name", "NetworkId", "Virtual IP", "Members");

            for (KubevirtLoadBalancer lb : lbs) {
                print(format,
                        StringUtils.substring(lb.name(),
                                0, CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                        StringUtils.substring(lb.networkId(),
                                0, CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                        StringUtils.substring(lb.vip().toString(),
                                0, CLI_IP_ADDRESS_LENGTH - CLI_MARGIN_LENGTH),
                        lb.members().toString());
            }
        }
    }

    private String json(List<KubevirtLoadBalancer> lbs) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (KubevirtLoadBalancer lb : lbs) {
            result.add(jsonForEntity(lb, KubevirtLoadBalancer.class));
        }

        return prettyJson(mapper, result.toString());
    }
}
