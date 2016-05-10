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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.*;

/**
 * Warden for tracking use of shared test cells.
 */
class Warden {

    private static final String CELL_NOT_NULL = "Cell name cannot be null";
    private static final String USER_NOT_NULL = "User name cannot be null";
    private static final String KEY_NOT_NULL = "User key cannot be null";
    private static final String UTF_8 = "UTF-8";

    private static final long TIMEOUT = 10; // 10 seconds
    private static final int MAX_MINUTES = 240; // 4 hours max
    private static final int MINUTE = 60_000; // 1 minute
    private static final int DEFAULT_MINUTES = 60;

    private final File log = new File("warden.log");

    private final File cells = new File("cells");
    private final File supported = new File(cells, "supported");
    private final File reserved = new File(cells, "reserved");

    private final Random random = new Random();

    /**
     * Creates a new cell warden.
     */
    Warden() {
        random.setSeed(System.currentTimeMillis());
        Timer timer = new Timer("cell-pruner", true);
        timer.schedule(new Reposessor(), MINUTE / 4, MINUTE / 2);
    }

    /**
     * Returns list of names of supported cells.
     *
     * @return list of cell names
     */
    Set<String> getCells() {
        String[] list = supported.list();
        return list != null ? ImmutableSet.copyOf(list) : ImmutableSet.of();
    }

    /**
     * Returns list of names of available cells.
     *
     * @return list of cell names
     */
    private Set<String> getAvailableCells() {
        Set<String> available = new HashSet<>(getCells());
        available.removeAll(getReservedCells());
        return ImmutableSet.copyOf(available);
    }

    /**
     * Returns list of names of reserved cells.
     *
     * @return list of cell names
     */
    private Set<String> getReservedCells() {
        String[] list = reserved.list();
        return list != null ? ImmutableSet.copyOf(list) : ImmutableSet.of();
    }

    /**
     * Returns reservation for the specified user.
     *
     * @param userName user name
     * @return cell reservation record or null if user does not have one
     */
    private Reservation currentUserReservation(String userName) {
        checkNotNull(userName, USER_NOT_NULL);
        for (String cellName : getReservedCells()) {
            Reservation reservation = currentCellReservation(cellName);
            if (reservation != null && userName.equals(reservation.userName)) {
                return reservation;
            }
        }
        return null;
    }

    /**
     * Returns the name of the user who reserved the given cell.
     *
     * @param cellName cell name
     * @return cell reservation record or null if cell is not reserved
     */
    Reservation currentCellReservation(String cellName) {
        checkNotNull(cellName, CELL_NOT_NULL);
        File cellFile = new File(reserved, cellName);
        if (!cellFile.exists()) {
            return null;
        }
        try (InputStream stream = new FileInputStream(cellFile)) {
            return new Reservation(new String(ByteStreams.toByteArray(stream), "UTF-8"));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get current user for cell " + cellName, e);
        }
    }

    /**
     * Reserves a cell for the specified user and their public access key.
     *
     * @param userName user name
     * @param sshKey   user ssh public key
     * @param minutes  number of minutes for reservation
     * @return reserved cell definition
     */
    synchronized String borrowCell(String userName, String sshKey, int minutes) {
        checkNotNull(userName, USER_NOT_NULL);
        checkNotNull(sshKey, KEY_NOT_NULL);
        checkArgument(minutes < MAX_MINUTES, "Number of minutes must be less than %d", MAX_MINUTES);
        long now = System.currentTimeMillis();
        Reservation reservation = currentUserReservation(userName);
        if (reservation == null) {
            checkArgument(minutes >= 0, "Number of minutes must be non-negative");
            Set<String> cells = getAvailableCells();
            checkState(!cells.isEmpty(), "No cells are presently available");
            String cellName = ImmutableList.copyOf(cells).get(random.nextInt(cells.size()));
            reservation = new Reservation(cellName, userName, now, minutes == 0 ? DEFAULT_MINUTES : minutes);
        } else if (minutes == 0) {
            // If minutes are 0, simply return the cell definition
            return getCellDefinition(reservation.cellName);
        } else {
            reservation = new Reservation(reservation.cellName, userName, now, minutes);
        }

        reserveCell(reservation);
        createCell(reservation, sshKey);
        log(userName, reservation.cellName, "borrowed for " + reservation.duration + " minutes");
        return getCellDefinition(reservation.cellName);
    }

    /**
     * Returns the specified cell for the specified user and their public access key.
     *
     * @param userName user name
     */
    synchronized void returnCell(String userName) {
        checkNotNull(userName, USER_NOT_NULL);
        Reservation reservation = currentUserReservation(userName);
        checkState(reservation != null, "User %s has no cell reservations", userName);

        unreserveCell(reservation);
        destroyCell(reservation);
        log(userName, reservation.cellName, "returned");
    }

    /**
     * Reserves the specified cell for the user the source file and writes the
     * specified content to the target file.
     *
     * @param reservation cell reservation record
     */
    private void reserveCell(Reservation reservation) {
        File cellFile = new File(reserved, reservation.cellName);
        try (FileOutputStream stream = new FileOutputStream(cellFile)) {
            stream.write(reservation.encode().getBytes(UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to reserve cell " + reservation.cellName, e);
        }
    }

    private String getCellDefinition(String cellName) {
        return exec("bin/cell-def " + cellName);
    }

    /**
     * Cancels the specified reservation.
     *
     * @param reservation reservation record
     */
    private void unreserveCell(Reservation reservation) {
        checkState(new File(reserved, reservation.cellName).delete(),
                   "Unable to return cell %s", reservation.cellName);
    }

    /**
     * Creates the cell for the specified user SSH key.
     *
     * @param reservation cell reservation
     * @param sshKey      ssh key
     */
    private void createCell(Reservation reservation, String sshKey) {
        CellInfo cellInfo = getCellInfo(reservation.cellName);
        String cmd = String.format("ssh %s warden/bin/create-cell %s %s %s",
                                   cellInfo.hostName, cellInfo.cellName,
                                   cellInfo.ipPrefix, sshKey);
        exec(cmd);
    }

    /**
     * Destroys the specified cell.
     *
     * @param reservation reservation record
     */
    private void destroyCell(Reservation reservation) {
        CellInfo cellInfo = getCellInfo(reservation.cellName);
        exec(String.format("ssh %s warden/bin/destroy-cell %s",
                           cellInfo.hostName, cellInfo.cellName));
    }

    /**
     * Reads the information about the specified cell.
     *
     * @param cellName cell name
     * @return cell information
     */
    private CellInfo getCellInfo(String cellName) {
        File cellFile = new File(supported, cellName);
        try (InputStream stream = new FileInputStream(cellFile)) {
            String[] fields = new String(ByteStreams.toByteArray(stream), UTF_8).split(" ");
            return new CellInfo(cellName, fields[0], fields[1]);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to definition for cell " + cellName, e);
        }
    }

    // Executes the specified command.
    private String exec(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            String output = new String(ByteStreams.toByteArray(process.getInputStream()), UTF_8);
            process.waitFor(TIMEOUT, TimeUnit.SECONDS);
            return process.exitValue() == 0 ? output : null;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to execute " + command);
        }
    }

    // Creates an audit log entry.
    private void log(String userName, String cellName, String action) {
        try (FileOutputStream fos = new FileOutputStream(log, true);
             PrintWriter pw = new PrintWriter(fos)) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            pw.println(String.format("%s\t%s\t%s\t%s", format.format(new Date()),
                                     userName, cellName, action));
            pw.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to log reservation action", e);
        }
    }

    // Carrier of cell information
    private final class CellInfo {
        final String cellName;
        final String hostName;
        final String ipPrefix;

        private CellInfo(String cellName, String hostName, String ipPrefix) {
            this.cellName = cellName;
            this.hostName = hostName;
            this.ipPrefix = ipPrefix;
        }
    }

    // Task for re-possessing overdue cells
    private final class Reposessor extends TimerTask {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            for (String cellName : getReservedCells()) {
                Reservation reservation = currentCellReservation(cellName);
                if (reservation != null &&
                        (reservation.time + reservation.duration * MINUTE) < now) {
                    try {
                        returnCell(reservation.userName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
