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
package org.onosproject.uonos.impl;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.onosproject.uonos.DeviceOuterClass;
import org.onosproject.uonos.DeviceServiceGrpc;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.uonos.DeviceOuterClass.ListRequest;
import static org.onosproject.uonos.DeviceOuterClass.ListResponse;

/**
 * Provides implementation of the gRPC interface for onos-topo service.
 */
@Component(immediate = true)
public class MicroOnosTopoManager {

    private static final int MICRO_ONOS_PORT = 5150;
    private static final byte[] MICRO_ONOS_DEFAULT_CERT = ("\n-----BEGIN CERTIFICATE-----\n" +
            "MIIDZTCCAk0CCQDl7NF6ekffcTANBgkqhkiG9w0BAQsFADByMQswCQYDVQQGEwJV\n" +
            "UzELMAkGA1UECAwCQ0ExEjAQBgNVBAcMCU1lbmxvUGFyazEMMAoGA1UECgwDT05G\n" +
            "MRQwEgYDVQQLDAtFbmdpbmVlcmluZzEeMBwGA1UEAwwVY2Eub3Blbm5ldHdvcmtp\n" +
            "bmcub3JnMB4XDTE5MDQxMTExMTYyM1oXDTIwMDQxMDExMTYyM1owdzELMAkGA1UE\n" +
            "BhMCVVMxCzAJBgNVBAgMAkNBMRIwEAYDVQQHDAlNZW5sb1BhcmsxDDAKBgNVBAoM\n" +
            "A09ORjEUMBIGA1UECwwLRW5naW5lZXJpbmcxIzAhBgNVBAMMGmNsaWVudDEub3Bl\n" +
            "bm5ldHdvcmtpbmcub3JnMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA\n" +
            "5mR12oGXP+uDD7DzQZdTg96eHWTc0UKPwie2I5LLLVsRoH2PO5s2B5r6r/E8OUG4\n" +
            "0pGb6tkDRIJ8eC0Z/6NvBkzn4fsJ5g0UW6sVlXfaf0y9JnMSvV05+g++75a7+CRx\n" +
            "1BG3GNjGWbke1mx8d6SrQ8D1sjI3L0D+32mi0WU9jO2Uw9YXvXgxQmL9Krxdr3M/\n" +
            "aZO9sTJZtIT0EEY3qBpPv+daAbuP5m+uhiEzYZP2bLywyzGyfrUmj9fjG/D1kuMM\n" +
            "haEIUJQ2VTcIApKG/Kb3Mk3b3VCfTvpEHMVrKMoyNHQXXi+6X106+cu2WtoPv+U5\n" +
            "VFVoufjRWSbcOmQ7qIHBiwIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBRBR6LTFEU\n" +
            "SWeEeguMsbHxN/6NIZuPejib1q9fTHeZ9cnIHIOLJaZzHiMZn5uw8s6D26kveNps\n" +
            "iCr4O8xOjUa0uwbhMTgm3wkODLlV1DwGjFWk8v5UKGWqUQ94wVMQ16YMIR5DgJJM\n" +
            "0DUzVcoFz+vLnMrDZ0AEk5vra1Z5KweSRvwHX7dJ6FIW7X3IgqXTqJtlV/D/vIi3\n" +
            "UfBnjzqOy2LVfBD7du7i5NbTHfTUpoTvddVwQaKCuQGYHocoQvQD3VQcQDh1u0DD\n" +
            "n2GkeEDLaDAGFAIO+PDg2iT8BhKeEepqswid9gYAhZcOjrlnl6smZo7jEzBj1a9Q\n" +
            "e3q1STjfQqe8\n" +
            "-----END CERTIFICATE-----\n").getBytes();

    private static final byte[] MICRO_ONOS_DEFAULT_KEY = ("\n-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC4zBmctGU/0LQ4\n" +
            "uJAtny0Zh/NwAVAgjAlVE9Y3hh1fTiXB7eAPhLrCa3KCzRlQAUCvorNPKYhlHV70\n" +
            "CL8ACWVrstxeY635I3UGanPZhdNRv1h6hW8hympqIKO1otWDV6pwi0gXo6Zco8QQ\n" +
            "/3LriBlsGn34kvP52Sh4n/id38CCw34OgkItfZb8VeSkXdNQHN1sfB/z0fJXGdIz\n" +
            "Io5JGGfgiw/i5ccesyXxfN8A3vBhKE1sUJzeHZGEzkdmo/ZbfpVQt4H6aKurYfwD\n" +
            "xDQjVasf+Lq+WDKd2OzN+BZ+YBMNWUcmZkAh8zZmQWhCttLL484GOeCtqq5HGsTD\n" +
            "JvfkHSAvAgMBAAECggEBAIi5ORnfvimA2FY+9y1J36xMEaiE0CvEcAMqMgvShljF\n" +
            "ENpyjJvur964cHimFlxDEQDhd5jSOb/WAzK6ZdY5HXiZVMHhLg5uVV7x09TUVozc\n" +
            "7TF5F8gAYssyau0wFJige9HYuvYCdkuEPsP0u6nXgDejQiBvWWM5b+APO3pS2bPk\n" +
            "fbyXPnp+xUQAQhH/m+VCCG4hlC1bKulkY4yWKp5wrKtj2Xun0Vh4Iu9UJiH/EuQ6\n" +
            "mwpUeWLECO5OYDKQWH90iIVDZufe+8yw1VZrN82cTWL86jetgPEXz6klWGUu1gHU\n" +
            "r57xz0Nb4rhFPs49aw9Yj/LswF1I5zdF6EiT0H5aRoECgYEA2tNBEo4ta3JW4wF/\n" +
            "/3AKAE79RV+06z74x3Id5w2ED8TRq3SRu5oMmR9kJhe6owNg8WcbK/h/LuI5+CPD\n" +
            "V8Uo8HGQs5VisSJGJdCul8gA2bDGPEokGezdEdE6UUaNxpgRU484owZau80c4Q/Y\n" +
            "B4evLdJL9KIaSB5oPHGfu65i41kCgYEA2DD29X6i1GOSwzKXTXNdrZjxxOqeZiVv\n" +
            "/+TTiRffIUNMObtR6B8wWi5Y+oPUUj+Xop1vM7L9ZkwfkuDMtaZbryA37rsoAKP9\n" +
            "Sdlemyt0cB+cL7MN04Od9UD4YzbapRGAoFJduzzneQN0PBUc6wvB6cMBH2UZsEjQ\n" +
            "GdV0r/iC1scCgYEA0pg9J/5s99syg4YOCWdqOKHMXded5kjUZB4PaS44ynRA1SF6\n" +
            "n3HCbhsn5wEvPXMi+TChlc+xlw1hfM3uUaoNnFmvSSWbtZ2mpP4RCUISj27xWVSB\n" +
            "KfIrT9pspYuhJl9zTVeoyjxzVgowoOj+n0CV9yNMtkLLyFx7NLClaZqK0QECgYAj\n" +
            "rECz1YeMwDlxWCG7N/QXNwt90LD+beMDOIDnODcrR+2GATDMuojB+K/Z9nLMd43P\n" +
            "2WaGA1zoylrTY6CjwKWUSh6wl9VL9cNPsjx4Ij1+WtjszgDUC/2+gE/8HwsI/dBZ\n" +
            "o/2vbadMQpOlbl5tMm124ySGR6prejhMavpsJvd/9QKBgGBN5jGWqdezvFx1dY0S\n" +
            "uZAlqQ3h5w/0MMGSmaM+yN30wjFdWd3yMAxNgIp4Oj1noqvuqXiXnBjN6YER3GNx\n" +
            "hpjimxkZY9ogyVGz+RP9YqGKKbUtBL/8zZ/LYbWGvo9yJ6HxwO397EhyXNhCvwyF\n" +
            "sDhPaK+DnzwfkjBk3kXNve4o\n" +
            "-----END PRIVATE KEY-----\n").getBytes();

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Server server;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Activate
    protected void activate() {
        try {
            server = NettyServerBuilder.forPort(MICRO_ONOS_PORT)
// FIXME: make this work later
//                    .useTransportSecurity(new ByteArrayInputStream(MICRO_ONOS_DEFAULT_CERT),
//                                          new ByteArrayInputStream(MICRO_ONOS_DEFAULT_KEY))
                    .addService(new MicroOnosDeviceService())
                    .build()
                    .start();
        } catch (IOException e) {
            log.error("Unable to start gRPC server", e);
            throw new IllegalStateException("Unable to start gRPC server", e);
        }
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        if (server != null) {
            server.shutdown();
        }
        log.info("Stopped");
    }

    private class MicroOnosDeviceService extends DeviceServiceGrpc.DeviceServiceImplBase {
        @Override
        public void list(ListRequest request,
                         StreamObserver<ListResponse> responseObserver) {
            for (Device d : deviceService.getDevices()) {
                Annotations annotations = d.annotations();
                DeviceOuterClass.Device.Builder db = DeviceOuterClass.Device.newBuilder()
                        .setId(d.id().toString())
                        .setVersion(d.swVersion())
                        .setType(d.type().toString());
                String value = annotations.value(AnnotationKeys.MANAGEMENT_ADDRESS);
                if (!isNullOrEmpty(value)) {
                    db.setAddress(value);
                }
                String role = annotations.value("role");
                if (!isNullOrEmpty(role)) {
                    db.setRole(role);
                }

                // TODO: populate protocols, etc.

                ListResponse resp = ListResponse.newBuilder()
                        .setType(ListResponse.Type.NONE)
                        .setDevice(db.build())
                        .build();
                responseObserver.onNext(resp);
            }
            responseObserver.onCompleted();
        }
    }

}
