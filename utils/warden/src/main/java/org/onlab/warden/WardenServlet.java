/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onlab.warden;

import com.google.common.io.ByteStreams;
import org.eclipse.jetty.server.Response;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Web socket servlet capable of creating web sockets for the STC monitor.
 */
public class WardenServlet extends HttpServlet {

    static Warden warden;

    private SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/plain; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            if (req.getPathInfo().endsWith("data")) {
                String userName = req.getParameter("user");
                if (userName != null) {
                    printUserInfo(out, userName);
                } else {
                    printAvailability(out);
                }
            } else {
                printAvailabilityText(out);
            }
        } catch (Exception e) {
            resp.setStatus(Response.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
    }

    private void printUserInfo(PrintWriter out, String userName) {
        Reservation reservation = warden.currentUserReservation(userName);
        out.println(getCellStatus(null, reservation));
    }

    private void printAvailability(PrintWriter out) {
        List<String> list = new ArrayList<>(warden.getCells());
        list.sort(String::compareTo);
        for (String cellName : list) {
            Reservation reservation = warden.currentCellReservation(cellName);
            out.println(getCellStatus(cellName, reservation));
        }
    }

    private String getCellStatus(String cellName, Reservation reservation) {
        if (reservation != null) {
            long expiration = reservation.time + reservation.duration * 60_000;
            long remaining = (expiration - System.currentTimeMillis()) / 60_000;
            return String.format("%s,%s,%s,%s", reservation.cellName,
                    reservation.cellSpec, reservation.userName, remaining);
        } else if (cellName != null) {
            return String.format("%s", cellName);
        }
        return null;
    }

    private void printAvailabilityText(PrintWriter out) {
        List<String> list = new ArrayList<>(warden.getCells());
        list.sort(String::compareTo);
        for (String cellName : list) {
            Reservation reservation = warden.currentCellReservation(cellName);
            if (reservation != null) {
                long expiration = reservation.time + reservation.duration * 60_000;
                long remaining = (expiration - System.currentTimeMillis()) / 60_000;
                out.println(String.format("%-14s\t%-10s\t%s\t%s\t%s mins (%s remaining)",
                                          cellName + "-" + reservation.cellSpec,
                                          reservation.userName,
                                          fmt.format(new Date(reservation.time)),
                                          fmt.format(new Date(expiration)),
                                          reservation.duration, remaining));
            } else {
                out.println(String.format("%-10s\t%-10s", cellName, "available"));
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (PrintWriter out = resp.getWriter()) {
            String sshKey = new String(ByteStreams.toByteArray(req.getInputStream()), "UTF-8");
            String userName = req.getParameter("user");
            String sd = req.getParameter("duration");
            String spec = req.getParameter("spec");
            int duration = isNullOrEmpty(sd) ? 0 : Integer.parseInt(sd);
            String cellDefinition = warden.borrowCell(userName, sshKey, duration, spec);
            out.println(cellDefinition);
        } catch (Exception e) {
            resp.setStatus(Response.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (PrintWriter out = resp.getWriter()) {
            String userName = req.getParameter("user");
            warden.returnCell(userName);
        } catch (Exception e) {
            resp.setStatus(Response.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
    }
}
