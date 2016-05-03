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
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.*;

/**
 * Warden for tracking use of shared test cells.
 */
class Warden {

    private static final String CELL_NOT_NULL = "Cell name cannot be null";
    private static final String USER_NOT_NULL = "User name cannot be null";
    private static final String KEY_NOT_NULL = "User key cannot be null";
    private static final String UTF_8 = "UTF-8";
    private static final long TIMEOUT = 3;

    private static final String AUTHORIZED_KEYS = "authorized_keys";

    private static final int MAX_MINUTES = 240; // 4 hours max
    private static final int MINUTE = 60_000; // 1 minute

    private final File log = new File("warden.log");

    private final File cells = new File("cells");
    private final File supported = new File(cells, "supported");
    private final File reserved = new File(cells, "reserved");

    private final Random random = new Random();

    private final Timer timer = new Timer("cell-pruner", true);

    /**
     * Creates a new cell warden.
     */
    Warden() {
        random.setSeed(System.currentTimeMillis());
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
    Set<String> getAvailableCells() {
        Set<String> available = new HashSet<>(getCells());
        available.removeAll(getReservedCells());
        return ImmutableSet.copyOf(available);
    }

    /**
     * Returns list of names of reserved cells.
     *
     * @return list of cell names
     */
    Set<String> getReservedCells() {
        String[] list = reserved.list();
        return list != null ? ImmutableSet.copyOf(list) : ImmutableSet.of();
    }


    /**
     * Returns reservation for the specified user.
     *
     * @param userName user name
     * @return cell reservation record or null if user does not have one
     */
    Reservation currentUserReservation(String userName) {
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
        checkArgument(minutes > 0, "Number of minutes must be positive");
        checkArgument(minutes < MAX_MINUTES, "Number of minutes must be less than %d", MAX_MINUTES);
        long now = System.currentTimeMillis();
        Reservation reservation = currentUserReservation(userName);
        if (reservation == null) {
            Set<String> cells = getAvailableCells();
            checkState(!cells.isEmpty(), "No cells are presently available");
            String cellName = ImmutableList.copyOf(cells).get(random.nextInt(cells.size()));
            reservation = new Reservation(cellName, userName, now, minutes);
        } else {
            reservation = new Reservation(reservation.cellName, userName, now, minutes);
        }

        reserveCell(reservation.cellName, reservation);
        installUserKeys(reservation.cellName, userName, sshKey);
        log(userName, reservation.cellName, "borrowed for " + minutes + " minutes");
        return getCellDefinition(reservation.cellName);
    }

    /**
     * Reserves the specified cell for the user the source file and writes the
     * specified content to the target file.
     *
     * @param cellName    cell name
     * @param reservation cell reservation record
     */
    private void reserveCell(String cellName, Reservation reservation) {
        try (FileOutputStream stream = new FileOutputStream(new File(reserved, cellName))) {
            stream.write(reservation.encode().getBytes(UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to reserve cell " + cellName, e);
        }
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
        checkState(new File(reserved, reservation.cellName).delete(),
                   "Unable to return cell %s", reservation.cellName);
        uninstallUserKeys(reservation.cellName);
        log(userName, reservation.cellName, "returned");
    }

    /**
     * Reads the definition of the specified cell.
     *
     * @param cellName cell name
     * @return cell definition
     */
    String getCellDefinition(String cellName) {
        File cellFile = new File(supported, cellName);
        try (InputStream stream = new FileInputStream(cellFile)) {
            return new String(ByteStreams.toByteArray(stream), UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to definition for cell " + cellName, e);
        }
    }

    // Returns list of cell hosts, i.e. OC#, OCN
    private List<String> cellHosts(String cellName) {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        Pattern pattern = Pattern.compile("export OC[0-9N]=(.*)");
        for (String line : getCellDefinition(cellName).split("\n")) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                builder.add(matcher.group(1).replaceAll("[\"']", ""));
            }
        }
        return builder.build();
    }

    // Installs the specified user's key on all hosts of the given cell.
    private void installUserKeys(String cellName, String userName, String sshKey) {
        File authKeysFile = authKeys(sshKey);
        for (String host : cellHosts(cellName)) {
            installAuthorizedKey(host, authKeysFile.getPath());
        }
        checkState(authKeysFile.delete(), "Unable to install user keys");
    }

    // Uninstalls the user keys on the specified cell
    private void uninstallUserKeys(String cellName) {
        for (String host : cellHosts(cellName)) {
            installAuthorizedKey(host, AUTHORIZED_KEYS);
        }
    }

    // Installs the authorized keys on the specified host.
    private void installAuthorizedKey(String host, String authorizedKeysFile) {
        String cmd = "scp " + authorizedKeysFile + " sdn@" + host + ":.ssh/authorized_keys";
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor(TIMEOUT, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to set authorized keys for host " + host);
        }
    }

    // Returns the file containing authorized keys that incudes the specified key.
    private File authKeys(String sshKey) {
        File keysFile = new File(AUTHORIZED_KEYS);
        try {
            File tmp = File.createTempFile("warden-", ".auth");
            tmp.deleteOnExit();
            try (InputStream stream = new FileInputStream(keysFile);
                 PrintWriter output = new PrintWriter(tmp)) {
                String baseKeys = new String(ByteStreams.toByteArray(stream), UTF_8);
                output.println(baseKeys);
                output.println(sshKey);
                return tmp;
            } catch (IOException e) {
                throw new IllegalStateException("Unable to generate authorized keys", e);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate authorized keys", e);
        }
    }

    // Creates an audit log entry.
    void log(String userName, String cellName, String action) {
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

    // Task for re-possessing overdue cells
    private class Reposessor extends TimerTask {
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
