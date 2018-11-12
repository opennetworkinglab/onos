/**
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.kafkaintegration.kafka;

import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.kafkaintegration.api.KafkaConfigService;
import org.onosproject.kafkaintegration.api.KafkaPublisherAdminService;
import org.onosproject.kafkaintegration.api.dto.KafkaServerConfig;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onosproject.kafkaintegration.kafka.OsgiPropertyConstants.*;

@Component(immediate = true, service = KafkaConfigService.class,
        property = {
                BOOTSTRAP_SERVERS + "=" + BOOTSTRAP_SERVERS_DEFAULT,
                RETRIES + ":Integer=" + RETRIES_DEFAULT,
                MAX_IN_FLIGHT + ":Integer=" + MAX_IN_FLIGHT_DEFAULT,
                REQUIRED_ACKS + ":Integer=" + REQUIRED_ACKS_DEFAULT,
                KEY_SERIALIZER + "=" + KEY_SERIALIZER_DEFAULT,
                VALUE_SERIALIZER + "=" + VALUE_SERIALIZER_DEFAULT,
        }
)
public class KafkaConfigManager implements KafkaConfigService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KafkaPublisherAdminService kafkaPublisherAdminService;

    /** Default IP/Port pair to establish initial connection to Kafka cluster. */
    protected String bootstrapServers = BOOTSTRAP_SERVERS_DEFAULT;

    /** Number of times the producer can retry to send after first failure. */
    protected int retries = RETRIES_DEFAULT;

    /** The maximum number of unacknowledged requests the client will send before blocking. */
    protected int maxInFlightRequestsPerConnection = MAX_IN_FLIGHT_DEFAULT;

    /** Producer will get an acknowledgement after the leader has replicated the data. */
    protected int requestRequiredAcks = REQUIRED_ACKS_DEFAULT;

    /** Serializer class for key that implements the Serializer interface. */
    protected String keySerializer = KEY_SERIALIZER_DEFAULT;

    /** Serializer class for value that implements the Serializer interface. */
    protected String valueSerializer = VALUE_SERIALIZER_DEFAULT;

    @Activate
    protected void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());
        kafkaPublisherAdminService.start(getConfigParams());
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);
        kafkaPublisherAdminService.stop();
        log.info("Stopped");
    }

    @Modified
    private void modified(ComponentContext context) {
        if (context == null) {
            bootstrapServers = BOOTSTRAP_SERVERS_DEFAULT;
            retries = RETRIES_DEFAULT;
            maxInFlightRequestsPerConnection = MAX_IN_FLIGHT_DEFAULT;
            requestRequiredAcks = REQUIRED_ACKS_DEFAULT;
            keySerializer = KEY_SERIALIZER_DEFAULT;
            valueSerializer = VALUE_SERIALIZER_DEFAULT;
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();

        String newBootstrapServers;
        int newRetries;
        int newMaxInFlightRequestsPerConnection;
        int newRequestRequiredAcks;
        try {
            String s = get(properties, BOOTSTRAP_SERVERS);
            newBootstrapServers =
                    isNullOrEmpty(s) ? bootstrapServers : s.trim();

            s = get(properties, RETRIES);
            newRetries =
                    isNullOrEmpty(s) ? retries : Integer.parseInt(s.trim());

            s = get(properties, MAX_IN_FLIGHT);
            newMaxInFlightRequestsPerConnection =
                    isNullOrEmpty(s) ? maxInFlightRequestsPerConnection
                                     : Integer.parseInt(s.trim());

            s = get(properties, REQUIRED_ACKS);
            newRequestRequiredAcks =
                    isNullOrEmpty(s) ? requestRequiredAcks
                                     : Integer.parseInt(s.trim());

        } catch (NumberFormatException | ClassCastException e) {
            return;
        }

        if (configModified(newBootstrapServers, newRetries,
                           newMaxInFlightRequestsPerConnection,
                           newRequestRequiredAcks)) {
            bootstrapServers = newBootstrapServers;
            String kafkaServerIp = bootstrapServers
                    .substring(0, bootstrapServers.indexOf(":"));
            String kafkaServerPortNum = bootstrapServers
                    .substring(bootstrapServers.indexOf(":") + 1);

            retries = newRetries;

            maxInFlightRequestsPerConnection =
                    newMaxInFlightRequestsPerConnection;

            requestRequiredAcks = newRequestRequiredAcks;

            kafkaPublisherAdminService.restart(KafkaServerConfig.builder()
                    .ipAddress(kafkaServerIp).port(kafkaServerPortNum)
                    .numOfRetries(retries)
                    .maxInFlightRequestsPerConnection(maxInFlightRequestsPerConnection)
                    .acksRequired(requestRequiredAcks)
                    .keySerializer(keySerializer)
                    .valueSerializer(valueSerializer).build());

            log.info("Kafka Server Config has been Modified - "
                    + "bootstrapServers {}, retries {}, "
                    + "maxInFlightRequestsPerConnection {}, "
                    + "requestRequiredAcks {}", bootstrapServers, retries,
                     maxInFlightRequestsPerConnection, requestRequiredAcks);
        } else {
            return;
        }
    }

    private boolean configModified(String newBootstrapServers, int newRetries,
                                   int newMaxInFlightRequestsPerConnection,
                                   int newRequestRequiredAcks) {

        return !newBootstrapServers.equals(bootstrapServers)
                || newRetries != retries
                || newMaxInFlightRequestsPerConnection != maxInFlightRequestsPerConnection
                || newRequestRequiredAcks != requestRequiredAcks;

    }

    @Override
    public KafkaServerConfig getConfigParams() {
        String ipAddr = bootstrapServers.substring(0, bootstrapServers.indexOf(":"));
        String port = bootstrapServers.substring(bootstrapServers.indexOf(":") + 1);

        return KafkaServerConfig.builder().ipAddress(ipAddr).port(port)
                .numOfRetries(retries)
                .maxInFlightRequestsPerConnection(maxInFlightRequestsPerConnection)
                .acksRequired(requestRequiredAcks).keySerializer(keySerializer)
                .valueSerializer(valueSerializer).build();

    }

}
