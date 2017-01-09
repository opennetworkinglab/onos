/**
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
package org.onosproject.kafkaintegration.kafka;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;

import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.kafkaintegration.api.KafkaConfigService;
import org.onosproject.kafkaintegration.api.KafkaPublisherAdminService;
import org.onosproject.kafkaintegration.api.dto.KafkaServerConfig;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service
public class KafkaConfigManager implements KafkaConfigService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected KafkaPublisherAdminService kafkaPublisherAdminService;

    public static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private String kafkaServerIp =
            BOOTSTRAP_SERVERS.substring(0, BOOTSTRAP_SERVERS.indexOf(":"));
    private String kafkaServerPortNum =
            BOOTSTRAP_SERVERS.substring(BOOTSTRAP_SERVERS.indexOf(":") + 1,
                                        BOOTSTRAP_SERVERS.length());

    private static final int RETRIES = 1;
    private static final int MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION = 5;
    private static final int REQUEST_REQUIRED_ACKS = 1;
    private static final String KEY_SERIALIZER =
            "org.apache.kafka.common.serialization.StringSerializer";
    private static final String VALUE_SERIALIZER =
            "org.apache.kafka.common.serialization.ByteArraySerializer";

    @Property(name = "bootstrap.servers", value = BOOTSTRAP_SERVERS,
            label = "Default IP/Port pair to establish initial connection to Kafka cluster.")
    protected String bootstrapServers = BOOTSTRAP_SERVERS;

    @Property(name = "retries", intValue = RETRIES,
            label = "Number of times the producer can retry to send after first failure")
    protected int retries = RETRIES;

    @Property(name = "max.in.flight.requests.per.connection",
            intValue = MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
            label = "The maximum number of unacknowledged requests the client will send before blocking")
    protected int maxInFlightRequestsPerConnection =
            MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION;

    @Property(name = "request.required.acks", intValue = 1,
            label = "Producer will get an acknowledgement after the leader has replicated the data")
    protected int requestRequiredAcks = REQUEST_REQUIRED_ACKS;

    @Property(name = "key.serializer", value = KEY_SERIALIZER,
            label = "Serializer class for key that implements the Serializer interface.")
    protected String keySerializer = KEY_SERIALIZER;

    @Property(name = "value.serializer", value = VALUE_SERIALIZER,
            label = "Serializer class for value that implements the Serializer interface.")
    protected String valueSerializer = VALUE_SERIALIZER;

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
            bootstrapServers = BOOTSTRAP_SERVERS;
            retries = RETRIES;
            maxInFlightRequestsPerConnection =
                    MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION;
            requestRequiredAcks = REQUEST_REQUIRED_ACKS;
            keySerializer = KEY_SERIALIZER;
            valueSerializer = VALUE_SERIALIZER;
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();

        String newBootstrapServers;
        int newRetries;
        int newMaxInFlightRequestsPerConnection;
        int newRequestRequiredAcks;
        try {
            String s = get(properties, "bootstrap.servers");
            newBootstrapServers =
                    isNullOrEmpty(s) ? bootstrapServers : s.trim();

            s = get(properties, "retries");
            newRetries =
                    isNullOrEmpty(s) ? retries : Integer.parseInt(s.trim());

            s = get(properties, "max.in.flight.requests.per.connection");
            newMaxInFlightRequestsPerConnection =
                    isNullOrEmpty(s) ? maxInFlightRequestsPerConnection
                                     : Integer.parseInt(s.trim());

            s = get(properties, "request.required.acks");
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
            kafkaServerIp = bootstrapServers
                    .substring(0, bootstrapServers.indexOf(":"));
            kafkaServerPortNum = bootstrapServers
                    .substring(bootstrapServers.indexOf(":") + 1,
                               bootstrapServers.length());

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
        String ipAddr =
                bootstrapServers.substring(0, bootstrapServers.indexOf(":"));
        String port =
                bootstrapServers.substring(bootstrapServers.indexOf(":") + 1,
                                           bootstrapServers.length());

        return KafkaServerConfig.builder().ipAddress(ipAddr).port(port)
                .numOfRetries(retries)
                .maxInFlightRequestsPerConnection(maxInFlightRequestsPerConnection)
                .acksRequired(requestRequiredAcks).keySerializer(keySerializer)
                .valueSerializer(valueSerializer).build();

    }

}
