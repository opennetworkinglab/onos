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

package org.onosproject.incubator.net.dpi.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.dpi.DpiStatInfo;
import org.onosproject.incubator.net.dpi.DpiStatistics;
import org.onosproject.incubator.net.dpi.DpiStatisticsManagerService;
import org.onosproject.incubator.net.dpi.FlowStatInfo;
import org.onosproject.incubator.net.dpi.ProtocolStatInfo;
import org.onosproject.incubator.net.dpi.TrafficStatInfo;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.sleep;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * DPI Statistics Manager.
 */
@Component(immediate = true)
@Service
public class DpiStatisticsManager implements DpiStatisticsManagerService {

    private static ServerSocket serverSocket;
    private static int port = 11990; // socket server listening port

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private ApplicationId appId;

    private final ExecutorService dpiListenerThread =
            Executors.newSingleThreadExecutor(groupedThreads("onos/apps/dpi", "dpi-listener"));

    DpiStatisticsListener dpiStatisticsListener = null;

    // 31*2(month)*24(hour)*3600(second)/5(second)
    private static final int MAX_DPI_STATISTICS_ENTRY = 1071360;

    private SortedMap<String, DpiStatistics> dpiStatisticsMap =
            new TreeMap<>(new MapComparator());

    private long convertTimeToLong(String timeString) {
        long timeLong = 0;

        try {
            // Time format: yyyy-MM-dd HH:mm:ss, Time Zone: GMT
            SimpleDateFormat df = new SimpleDateFormat(DATE_FMT, Locale.KOREA);
            df.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));

             timeLong = df.parse(timeString).getTime();
        } catch (ParseException e) {
            log.error("Time parse error! Exception={}", e.toString());
        }

        return timeLong;
    }

    private static final String DATE_FMT = "yyyy-MM-dd HH:mm:ss";
    private static final String TIME_ZONE = "GMT";

    public static final int MAX_DPI_STATISTICS_REQUEST = 100;
    public static final int MAX_DPI_STATISTICS_TOPN = 100;

    @Activate
    public void activate(ComponentContext context) {
        appId = coreService.registerApplication("org.onosproject.dpi");

        dpiStatisticsListener = new DpiStatisticsListener();
        dpiListenerThread.execute(dpiStatisticsListener);

        log.info("Started", appId.id());
    }

    @Deactivate
    public void deactivate() {
        log.info("Deactivated...");
        dpiStatisticsListener.stop();
        dpiListenerThread.shutdown();
        log.info("Stopped");
    }

    @Override
    public DpiStatistics getDpiStatisticsLatest() {
        if (dpiStatisticsMap.size() > 0) {
            return dpiStatisticsMap.get(dpiStatisticsMap.firstKey());
        } else {
            return null;
        }
    }

    @Override
    public DpiStatistics getDpiStatisticsLatest(int topnProtocols, int topnFlows) {
        DpiStatistics ds, topnDs;

        ds = getDpiStatisticsLatest();
        topnDs = processTopn(ds, topnProtocols, topnFlows);

        return topnDs;
    }

    @Override
    public List<DpiStatistics> getDpiStatistics(int lastN) {
        List<DpiStatistics> dsList = new ArrayList<>();
        DpiStatistics ds;

        if (lastN > MAX_DPI_STATISTICS_REQUEST) {
            lastN = MAX_DPI_STATISTICS_REQUEST;
        }

        SortedMap tempMap = new TreeMap(new MapComparator());
        tempMap.putAll(dpiStatisticsMap);

        for (int i = 0; i < lastN && i < tempMap.size(); i++) {
            ds = (DpiStatistics) tempMap.get(tempMap.firstKey());
            dsList.add(i, new DpiStatistics(ds.receivedTime(), ds.dpiStatInfo()));

            tempMap.remove(tempMap.firstKey());
        }

        return dsList;
    }

    @Override
    public List<DpiStatistics> getDpiStatistics(int lastN, int topnProtocols, int topnFlows) {
        List<DpiStatistics> dsList;
        List<DpiStatistics> topnDsList = new ArrayList<>();
        DpiStatistics ds, topnDs;

        dsList = getDpiStatistics(lastN);
        for (int i = 0; i < dsList.size(); i++) {
            ds = dsList.get(i);
            topnDs = processTopn(ds, topnProtocols, topnFlows);
            topnDsList.add(i, topnDs);
        }

        return topnDsList;
    }

    @Override
    public DpiStatistics getDpiStatistics(String receivedTime) {
        DpiStatistics ds;

        if (receivedTime == null) {
            return null;
        }

        if (!dpiStatisticsMap.containsKey(receivedTime)) {
            return null;
        }

        ds = dpiStatisticsMap.get(receivedTime);

        return ds;
    }

    @Override
    public DpiStatistics getDpiStatistics(String receivedTime, int topnProtocols, int topnFlows) {
        DpiStatistics ds, topnDs;

        ds = getDpiStatistics(receivedTime);

        topnDs = processTopn(ds, topnProtocols, topnFlows);

        return topnDs;
    }

    @Override
    public DpiStatistics addDpiStatistics(DpiStatistics ds) {
        if (ds == null) {
            return ds;
        }

        // check the time. The firstKey is lastTime because of descending sorted order
        if (dpiStatisticsMap.size() > 0) {
            String lastTime = dpiStatisticsMap.get(dpiStatisticsMap.firstKey()).receivedTime();
            String inputTime = ds.receivedTime();

            long lastTimeLong = convertTimeToLong(lastTime);
            long inputTimeLong = convertTimeToLong(inputTime);

            if (lastTimeLong >= inputTimeLong) {
                return null;
            }
        }

        if (dpiStatisticsMap.size() >= MAX_DPI_STATISTICS_ENTRY) {
            // remove the last (oldest) entry
            dpiStatisticsMap.remove(dpiStatisticsMap.lastKey());
        }

        if (dpiStatisticsMap.containsKey(ds.receivedTime())) {
            log.warn("addDpiStatistics(), {} dpiStatistics is already existing!",
                      ds.receivedTime());
            return null;
        }

        dpiStatisticsMap.put(ds.receivedTime(), ds);
        log.debug("addDpiStatistics: dpiResultJson data[time={}] is added " +
                          "into DpiStatisticsMap size={}.",
                  ds.receivedTime(), dpiStatisticsMap.size());

        return ds;
    }

    private class MapComparator implements Comparator<String> {
        @Override
        public int compare(String rt1, String rt2) {
            long rt1Long = convertTimeToLong(rt1);
            long rt2Long = convertTimeToLong(rt2);

            // Descending order
            if (rt1Long > rt2Long) {
                return -1;
            } else if (rt1Long < rt2Long) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private class ProtocolComparator implements Comparator<ProtocolStatInfo> {
        @Override
        public int compare(ProtocolStatInfo p1, ProtocolStatInfo p2) {
            //Descending order
            if (p1.bytes() > p2.bytes()) {
                return -1;
            } else if (p1.bytes() < p2.bytes()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private class FlowComparator implements Comparator<FlowStatInfo> {
        @Override
        public int compare(FlowStatInfo f1, FlowStatInfo f2) {
            // Descending order
            if (f1.bytes() > f2.bytes()) {
                return -1;
            } else if (f1.bytes() < f2.bytes()) {
                return 1;
            } else {
                return 0;
            }
        }
    }
    private DpiStatistics processTopn(DpiStatistics ds, int topnProtocols, int topnFlows) {
        if (ds == null) {
            return null;
        }

        if (topnProtocols <= 0) {
            // displays all entries
            topnProtocols = 0;
        } else if (topnProtocols > MAX_DPI_STATISTICS_TOPN) {
            topnProtocols = MAX_DPI_STATISTICS_TOPN;
        }

        if (topnFlows <= 0) {
            // displays all entries
            topnFlows = 0;
        } else if (topnFlows > MAX_DPI_STATISTICS_TOPN) {
            topnFlows = MAX_DPI_STATISTICS_TOPN;
        }

        if (topnProtocols == 0 && topnFlows == 0) {
            return ds;
        }

        TrafficStatInfo tsi = ds.dpiStatInfo().trafficStatistics();
        List<ProtocolStatInfo> psiList;
        List<FlowStatInfo> kfList;
        List<FlowStatInfo> ufList;

        List<ProtocolStatInfo> pList = ds.dpiStatInfo().detectedProtos();
        Collections.sort(pList, new ProtocolComparator());
        if (topnProtocols > 0 && topnProtocols < pList.size()) {
            psiList = pList.subList(0, topnProtocols);
        } else {
            psiList = pList;
        }


        List<FlowStatInfo> fList = ds.dpiStatInfo().knownFlows();
        Collections.sort(fList, new FlowComparator());
        if (topnFlows > 0 && topnFlows < fList.size()) {
            kfList = fList.subList(0, topnFlows);
        } else {
            kfList = fList;
        }

        fList = ds.dpiStatInfo().unknownFlows();
        Collections.sort(fList, new FlowComparator());
        if (topnFlows > 0 && topnFlows < fList.size()) {
            ufList = fList.subList(0, topnFlows);
        } else {
            ufList = fList;
        }

        DpiStatInfo dsi = new DpiStatInfo();
        dsi.setTrafficStatistics(tsi);
        dsi.setDetectedProtos(psiList);
        dsi.setKnownFlows(kfList);
        dsi.setUnknownFlows(ufList);

        DpiStatistics retDs = new DpiStatistics(ds.receivedTime(), dsi);
        return retDs;
    }

    /**
     * Receiving DPI Statistics result thread.
     */
    private class DpiStatisticsListener implements Runnable {
        Socket clientSocket = null;
        BufferedReader in = null;
        PrintWriter out = null;

        String resultJsonString = null;

        static final int MAX_SLEEP_COUNT = 10;
        int sleepCount = 0;

        @Override
        public void run() {
            log.info("DpiStatisticsListener: Receiving thread started...");
            receiveDpiResult();
        }

        public void stop() {
            try {
                if (serverSocket != null) {
                    if (clientSocket != null) {
                        if (in != null) {
                            in.close();
                        }
                        if (out != null) {
                            out.close();
                        }
                        clientSocket.close();
                        //log.debug("DpiResultListener: stop(): Socket close() is done...");
                    }
                    serverSocket.close();
                    //log.debug("DpiResultListener: stop(): Server close() is done...");
                }
            } catch (Exception e) {
                log.error("DpiStatisticsListener: stop(): Server Socket closing error, exception={}",
                          e.toString());
            }
            log.debug("DpiStatisticsListener: stop(): stopped...");
        }

        private void receiveDpiResult() {
            try {
                serverSocket = new ServerSocket(port);
            } catch (Exception e) {
                log.error("DpiStatisticsListener: ServerSocket listening error from port={} in localhost, exception={}",
                          port, e.toString());
                return;
            }

            try {
                while (true) {
                    if (clientSocket == null) {
                        log.info("DpiStatisticsListener: Waiting for accepting from dpi client...");
                        clientSocket = serverSocket.accept();
                        log.info("DpiStatisticsListener: Accepted from dpi client={}",
                                 clientSocket.getRemoteSocketAddress().toString());

                        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        out = new PrintWriter(clientSocket.getOutputStream(), true); // For disconnecting check!

                        resultJsonString = null;
                    }

                    sleepCount = 0;
                    while (!in.ready()) {
                        sleep(1000); // sleep one second.
                        if (out.checkError() || ++sleepCount >= MAX_SLEEP_COUNT) {
                            log.debug("DpiStatisticsListener: server and socket connect is lost...");
                            in.close();
                            in = null;
                            out.close();
                            out = null;
                            clientSocket.close();
                            clientSocket = null;

                            break;
                        }
                    }

                    if (in != null) {
                        resultJsonString = in.readLine();

                        // process the result
                        log.trace("DpiStatisticsListener: resultJsonString={}", resultJsonString);
                        processResultJson(resultJsonString);
                    }
                }
            } catch (Exception e) {
                log.error("DpiStatisticsListener: Exception = {}", e.toString());
                return;
            }
        }

        private void processResultJson(String resultJsonString) {
            Date tr = new Date(System.currentTimeMillis());
            SimpleDateFormat df = new SimpleDateFormat(DATE_FMT, Locale.KOREA);
            df.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));

            String curReceivedTime = new String(df.format(tr));
            String curResultJson = new String(resultJsonString);

            DpiStatInfo dpiStatInfo;
            ObjectMapper mapper = new ObjectMapper();
            try {
                dpiStatInfo = mapper.readValue(curResultJson, DpiStatInfo.class);
            } catch (IOException e) {
                log.error("DpiStatisticsListener: ObjectMapper Exception = {}", e.toString());
                return;
            }

            DpiStatistics dpiStatistics = new DpiStatistics(curReceivedTime, dpiStatInfo);

            addDpiStatistics(dpiStatistics);
        }
    }
}
