/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.gluon.rsc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.config.NetworkConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.gluon.manager.GluonManager.addServer;
import static org.onosproject.gluon.manager.GluonManager.deleteServer;
import static org.onosproject.gluon.manager.GluonManager.getAllServersIP;
import static org.onosproject.gluon.rsc.GluonConstants.ACTION_DEL;
import static org.onosproject.gluon.rsc.GluonConstants.ACTION_GET;
import static org.onosproject.gluon.rsc.GluonConstants.ACTION_SET;
import static org.onosproject.gluon.rsc.GluonConstants.ACTIVE_SERVER;
import static org.onosproject.gluon.rsc.GluonConstants.BATCH_PROCESSING;
import static org.onosproject.gluon.rsc.GluonConstants.BATCH_QUERING;
import static org.onosproject.gluon.rsc.GluonConstants.BATCH_RECEIVED;
import static org.onosproject.gluon.rsc.GluonConstants.BATCH_SERVICE_STATUS;
import static org.onosproject.gluon.rsc.GluonConstants.BATCH_STOPPED;
import static org.onosproject.gluon.rsc.GluonConstants.DATA_REMOVED;
import static org.onosproject.gluon.rsc.GluonConstants.DATA_UPDATED;
import static org.onosproject.gluon.rsc.GluonConstants.E_BATCH_PROCESSING;
import static org.onosproject.gluon.rsc.GluonConstants.E_BATCH_PROCESSING_URL;
import static org.onosproject.gluon.rsc.GluonConstants.E_CLIENT_STOP;
import static org.onosproject.gluon.rsc.GluonConstants.E_REAL_TIME_PROCESSING;
import static org.onosproject.gluon.rsc.GluonConstants.E_SUBKEYS_PROCESSING;
import static org.onosproject.gluon.rsc.GluonConstants.GLUON_ACTION;
import static org.onosproject.gluon.rsc.GluonConstants.GLUON_CREATE_INDEX;
import static org.onosproject.gluon.rsc.GluonConstants.GLUON_KEY;
import static org.onosproject.gluon.rsc.GluonConstants.GLUON_MOD_INDEX;
import static org.onosproject.gluon.rsc.GluonConstants.GLUON_NODE;
import static org.onosproject.gluon.rsc.GluonConstants.GLUON_NODES;
import static org.onosproject.gluon.rsc.GluonConstants.GLUON_VALUE;
import static org.onosproject.gluon.rsc.GluonConstants.INVALID_ACTION;
import static org.onosproject.gluon.rsc.GluonConstants.KEYS;
import static org.onosproject.gluon.rsc.GluonConstants.MODE_START;
import static org.onosproject.gluon.rsc.GluonConstants.MODE_STOP;
import static org.onosproject.gluon.rsc.GluonConstants.NO_SERVER_AVAIL;
import static org.onosproject.gluon.rsc.GluonConstants.NO_SUBKEYS_AVAIL;
import static org.onosproject.gluon.rsc.GluonConstants.PROCESSING_FAILED;
import static org.onosproject.gluon.rsc.GluonConstants.PROTON;
import static org.onosproject.gluon.rsc.GluonConstants.REAL_TIME_PROCESSING;
import static org.onosproject.gluon.rsc.GluonConstants.REAL_TIME_RECEIVED;
import static org.onosproject.gluon.rsc.GluonConstants.REAL_TIME_SERVICE_STATUS;
import static org.onosproject.gluon.rsc.GluonConstants.SERVER_RUNNING;
import static org.onosproject.gluon.rsc.GluonConstants.SERVER_STOPPED;
import static org.onosproject.gluon.rsc.GluonConstants.STATUS_CODE;
import static org.onosproject.gluon.rsc.GluonConstants.SUBKEYS_RECEIVED;


public class GluonServer {

    private String protonKeyUri;
    private String serverUri;

    private CloseableHttpAsyncClient httpClient;

    //store gluon server supported subkeys
    private List<String> subKeys = new LinkedList<>();

    // Lists of gluon servers
    public Map<String, GluonServer> serverMap = getAllServersIP();

    private final Logger log = LoggerFactory.getLogger(getClass());

    // Real time executor thread
    private final ExecutorService executorRealTimeService = Executors
            .newSingleThreadExecutor(groupedThreads("EtcdRealTimeMonitor",
                                                    "executor-%d", log));
    // Batch executor thread
    private final ExecutorService executorBatchService = Executors
            .newSingleThreadExecutor(groupedThreads("EtcdBatchMonitor",
                                                    "executor-%d", log));

    // Statistics counter
    private int setCount = 0;
    private int delCount = 0;
    private int getCount = 0;
    // Server etcd version
    public String version;

    /**
     * To get Gluon server running version, needs to create at-least one object.
     */
    public GluonServer() {
    }

    /**
     * Realising server functionality.
     *
     * @param etcduri         server url
     * @param targetProtonKey server key type, default net-l3vpn
     * @param mode            server mode start or stop
     * @param version         running server version
     */
    public GluonServer(String etcduri, String targetProtonKey,
                       String mode, String version) {
        this.version = version;

        switch (mode) {
            // Handling stop mode
            case MODE_STOP:
                // return if server is not available into the server list
                if (!serverMap.containsKey(etcduri)) {
                    log.debug(NO_SERVER_AVAIL);
                    return;
                }
                try {
                    // stop batch service executor thread
                    log.debug(BATCH_SERVICE_STATUS,
                              executorBatchService.isShutdown());
                    executorBatchService.shutdown();
                    // stop real time service executor thread
                    log.debug(REAL_TIME_SERVICE_STATUS,
                              executorRealTimeService.isShutdown());
                    executorRealTimeService.shutdown();
                    // closing http client
                    httpClient.close();
                } catch (IOException io) {
                    log.error(E_CLIENT_STOP, io.getMessage());
                }
                // deletes server from gluon server list
                deleteServer(etcduri);
                log.debug(SERVER_STOPPED);
                return;
            // Handling start mode
            case MODE_START:
                if (serverMap.containsKey(etcduri)) {
                    //Returns user CLI if server is already running
                    // and logs all server info into log files
                    log.info(SERVER_RUNNING);
                    log.debug(ACTIVE_SERVER, serverMap.size());
                    return;
                }
                // Store gluon manager object and gluon server url
                addServer(etcduri, this);
                // Preparing server uri
                serverUri = etcduri + "/v2" + KEYS;
                // Preparing server subkeys uri
                protonKeyUri = PROTON + targetProtonKey;
                // Starts http client
                RequestConfig requestConfig = RequestConfig.custom().build();
                httpClient = HttpAsyncClients.custom()
                        .setDefaultRequestConfig(requestConfig).build();
                httpClient.start();

                // Start thread to handle and process RealTime data
                handleRealTimeData(null);

                // Start thread to handle and process batch data,
                // iff subkeys are available
                getAllProtonSubkeys(serverUri + protonKeyUri);
                if (getProtonSubkeys().isEmpty()) {
                    log.debug(NO_SUBKEYS_AVAIL);
                    return;
                }
                // handle RealTime data
                handleBatchData(0);
                return;
            default:
                log.debug(INVALID_ACTION);

        }
    }

    /**
     * Handles real time data which is received from Gluon server.
     *
     * @param index, It will be used in recursive call of
     *               real time monitoring method.
     *               modified index receive from GluonConfig config file
     */
    private void handleRealTimeData(Long index) {
        String realTimeUri = serverUri + protonKeyUri +
                "/?wait=true&recursive=true";
        if (index != null) {
            realTimeUri += "&waitIndex=" + index;
        }
        HttpGet request = new HttpGet(URI.create(realTimeUri));
        log.info(REAL_TIME_PROCESSING, realTimeUri);
        // Starts real time executor thread
        executorRealTimeService.execute(new Runnable() {
            public void run() {
                try {
                    httpClient.execute(
                            request, new FutureCallback<HttpResponse>() {

                                @Override
                                public void completed(HttpResponse result) {
                                    StatusLine statusLine =
                                            result.getStatusLine();
                                    int statusCode = statusLine.getStatusCode();
                                    if (statusCode ==
                                            STATUS_CODE &&
                                            result.getEntity() != null) {
                                        try {
                                            String json = EntityUtils
                                                    .toString(result.getEntity());
                                            GluonConfig response =
                                                    processRealTimeResponse(json);
                                            // Recursive call to handle
                                            // real time data
                                            handleRealTimeData(
                                                    response.modifiedIndex + 1);
                                        } catch (IOException e) {
                                            failed(e);
                                        }
                                    } else {
                                        log.error(E_REAL_TIME_PROCESSING);
                                    }
                                }

                                @Override
                                public void cancelled() {
                                    log.debug("Nothing to do with " +
                                                      "this overridden method");
                                }

                                @Override
                                public void failed(Exception e) {
                                    log.error(E_REAL_TIME_PROCESSING,
                                              e.getMessage());
                                }
                            });
                } catch (Exception e) {
                    log.error(E_REAL_TIME_PROCESSING, e.getMessage());
                }
            }
        });
    }


    /**
     * Handles batch data which is received from Gluon server.
     *
     * @param subKeyIndex gets all proton subkey value
     */
    private void handleBatchData(int subKeyIndex) {
        String currBatchUri = serverUri + getProtonSubkeys().get(subKeyIndex);
        HttpGet request = new HttpGet(URI.create(currBatchUri));

        if (0 == subKeyIndex) {
            log.debug(BATCH_PROCESSING, protonKeyUri);
        }
        log.info(BATCH_QUERING, currBatchUri);
        // Starts batch executor thread
        executorBatchService.execute(new Runnable() {
            public void run() {
                try {
                    httpClient.execute(request, new FutureCallback<HttpResponse>() {
                        @Override
                        public void completed(HttpResponse result) {
                            StatusLine statusLine = result.getStatusLine();
                            int statusCode = statusLine.getStatusCode();
                            if (statusCode == STATUS_CODE &&
                                    result.getEntity() != null) {
                                try {
                                    String json = EntityUtils
                                            .toString(result.getEntity());
                                    processBatchResponse(json);
                                    // Stop batch executor thread
                                    // once all gluon server subkeys processed
                                    if (subKeyIndex ==
                                            ((getProtonSubkeys().size()) - 1)) {
                                        cancelled();
                                        return;
                                    }

                                    handleBatchData(subKeyIndex + 1);
                                } catch (IOException e) {
                                    failed(e);
                                }
                            } else {
                                log.error(E_BATCH_PROCESSING_URL, currBatchUri);
                            }
                        }

                        @Override
                        public void cancelled() {
                            executorBatchService.shutdown();
                            log.debug(BATCH_STOPPED, protonKeyUri);
                        }

                        @Override
                        public void failed(Exception e) {
                            log.error(E_BATCH_PROCESSING, e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    log.error(E_BATCH_PROCESSING, e.getMessage());
                }
            }
        });
    }

    /**
     * Parse and process real time json data which is received from Gluon server.
     *
     * @param result real time json data
     * @return GluonConfig response
     */
    public GluonConfig processRealTimeResponse(String result) {
        ObjectMapper mapper = new ObjectMapper();
        GluonConfig response = null;
        try {
            log.info(REAL_TIME_RECEIVED, result);
            JsonNode jsonNode = mapper.readTree(result);
            String action = jsonNode.get(GLUON_ACTION).asText();
            String key = jsonNode.get(GLUON_NODE).get(GLUON_KEY).asText();
            long mIndex = jsonNode.get(GLUON_NODE)
                    .get(GLUON_MOD_INDEX).asLong();
            long cIndex = jsonNode.get(GLUON_NODE)
                    .get(GLUON_CREATE_INDEX).asLong();
            if (action.equals(ACTION_SET)) {
                String value = jsonNode.get(GLUON_NODE)
                        .get(GLUON_VALUE).asText();
                JsonNode modifyValue = mapper.readTree(value.replace("\\", ""));
                response = new GluonConfig(action, key, modifyValue, mIndex,
                                           cIndex);
                setCount++;
            } else if (action.equals(ACTION_DEL)) {
                response = new GluonConfig(action, key, null, mIndex, cIndex);
                delCount++;
            } else {
                log.debug(INVALID_ACTION);
            }
        } catch (IOException e) {
            log.error(E_REAL_TIME_PROCESSING, e.getMessage());
        }
        processEtcdResponse(response);
        return response;
    }

    /**
     * Parse and process batch json data which is received from Gluon server.
     *
     * @param result batch json data
     * @return GluonConfig response
     */
    public GluonConfig processBatchResponse(String result) {
        ObjectMapper mapper = new ObjectMapper();
        GluonConfig response = null;
        try {
            log.debug(BATCH_RECEIVED, result);
            JsonNode jsonNode = mapper.readTree(result);
            log.info("JSON NODE VALUE ARE: {}", jsonNode);
            String action = jsonNode.get(GLUON_ACTION).asText();
            JsonNode nodes = jsonNode.get(GLUON_NODE).get(GLUON_NODES);
            if (null != nodes) {
                for (JsonNode confNode : nodes) {
                    String key = confNode.get(GLUON_KEY).asText();
                    long mIndex = confNode.get(GLUON_MOD_INDEX).asLong();
                    long cIndex = confNode.get(GLUON_CREATE_INDEX).asLong();
                    String value = confNode.get(GLUON_VALUE).asText();
                    log.info("JSON NODE VALUE ARE 2: {}", value);
                    JsonNode modifyValue = mapper.readTree(value.replace("\\", ""));
                    log.info("JSON NODE MODIFY VALUE ARE 2: {}", modifyValue);
                    response = new GluonConfig(action, key,
                                               modifyValue, mIndex, cIndex);
                    getCount++;
                    processEtcdResponse(response);

                }
            }
        } catch (IOException e) {
            log.error(E_BATCH_PROCESSING, e.getMessage());
        }
        return response;
    }

    /**
     * Gets all the proton subkeys from Gluon server.
     *
     * @param subKeyUrl get every proton subkey Url
     */
    public void getAllProtonSubkeys(String subKeyUrl) {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(subKeyUrl);
        ObjectMapper mapper = new ObjectMapper();
        try {
            HttpResponse result = client.execute(request);
            StatusLine statusLine = result.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == STATUS_CODE && result.getEntity() != null) {
                String json = EntityUtils
                        .toString(result.getEntity());
                log.debug(SUBKEYS_RECEIVED, json);
                JsonNode jsonNode = mapper.readTree(json);
                JsonNode nodes = jsonNode.get(GLUON_NODE).get(GLUON_NODES);

                for (JsonNode confNode : nodes) {
                    String key = confNode.get(GLUON_KEY).asText();
                    storeProtonSubkey(key);
                }
            }
        } catch (IOException e) {
            log.error(E_SUBKEYS_PROCESSING, subKeyUrl);
        }
        return;
    }

    /**
     * Gets all the proton subkeys from Gluon server.
     *
     * @param uri get every proton subkey Url
     * @return version server version
     */
    public String getGluonServerVersion(String uri) {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(uri);
        ObjectMapper mapper = new ObjectMapper();
        String version = null;
        try {
            HttpResponse result = client.execute(request);
            StatusLine statusLine = result.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == STATUS_CODE && result.getEntity() != null) {
                String json = EntityUtils
                        .toString(result.getEntity());
                JsonNode jsonNode = mapper.readTree(json);
                version = jsonNode.get("etcdserver").asText();
            }
        } catch (IOException e) {
            log.error(PROCESSING_FAILED);
        }
        return version;
    }

    /**
     * Gluon data updating and deleting into/from NetworkConfig datastore.
     * config.apply will raise GluonConfig.class event for add,
     * get and delete operations.
     *
     * @param gluonConfigMessage Etcdresponse data after parsing
     */
    public void processEtcdResponse(GluonConfig gluonConfigMessage) {

        NetworkConfigService configService =
                DefaultServiceDirectory.getService(NetworkConfigService.class);
        if (gluonConfigMessage.action.equals(ACTION_SET) ||
                gluonConfigMessage.action.equals(ACTION_GET)) {
            GluonConfig config = configService
                    .addConfig(gluonConfigMessage.key, GluonConfig.class);
            config.setEtcdResponse(gluonConfigMessage);
            config.apply();
            log.info(DATA_UPDATED);
        } else if (gluonConfigMessage.action.equals(ACTION_DEL)) {
            configService.removeConfig(gluonConfigMessage.key,
                                       GluonConfig.class);
            log.info(DATA_REMOVED);
        } else {
            log.info(INVALID_ACTION);
        }
    }

    /**
     * Returns set statistics.
     *
     * @return setCount
     */
    public int getSetCount() {
        return setCount;
    }

    /**
     * Returns get statistics.
     *
     * @return getCount
     */
    public int getGetCount() {
        return getCount;
    }

    /**
     * Returns delete statistics.
     *
     * @return delCount
     */
    public int getDelCount() {
        return delCount;
    }

    /**
     * Returns proton subkeys.
     *
     * @return subkeys
     */
    public List<String> getProtonSubkeys() {
        return subKeys;
    }

    /**
     * store proton subkeys.
     *
     * @param keys proton subkey
     */
    public void storeProtonSubkey(String keys) {
        subKeys.add(keys);
    }
}

