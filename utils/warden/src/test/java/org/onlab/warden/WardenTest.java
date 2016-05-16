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

import com.google.common.io.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.util.Tools;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.Assert.*;

/**
 * Suite of tests for the cell warden.
 */
public class WardenTest {

    private Warden warden;
    private File cells;
    private File supportedCells;

    @Before
    public void setUp() throws IOException {
        // Setup warden to be tested
        Warden.root = Files.createTempDir();
        Warden.cmdPrefix = "echo ";
        cells = new File(Warden.root, "cells");
        supportedCells = new File(cells, "supported");
        warden = new Warden();

        // Setup test cell information
        createCell("alpha", "foo");
        createCell("bravo", "foo");
        createCell("charlie", "foo");
        createCell("delta", "bar");
        createCell("echo", "bar");
        createCell("foxtrot", "bar");

        new File("warden.log").deleteOnExit();
    }

    private void createCell(String cellName, String hostName) throws IOException {
        File cellFile = new File(supportedCells, cellName);
        Files.createParentDirs(cellFile);
        Files.write((hostName + " " + cellName).getBytes(), cellFile);
    }

    @After
    public void tearDown() throws IOException {
        Tools.removeDirectory(Warden.root);
    }

    @Test
    public void basics() {
        assertEquals("incorrect number of cells", 6, warden.getCells().size());
        validateSizes(6, 0);

        String cellDefinition = warden.borrowCell("dude", "the-key", 0, null);
        assertTrue("incorrect definition", cellDefinition.contains("cell-def"));
        validateSizes(5, 1);

        Reservation dudeCell = warden.currentUserReservation("dude");
        validateCellState(dudeCell);

        warden.borrowCell("dolt", "a-key", 0, "4+1");
        Reservation doltCell = warden.currentUserReservation("dolt");
        validateCellState(doltCell);
        validateSizes(4, 2);

        assertFalse("cells should not be on the same host",
                    Objects.equals(warden.getCellHost(dudeCell.cellName),
                                   warden.getCellHost(doltCell.cellName)));

        warden.returnCell("dude");
        validateSizes(5, 1);

        warden.borrowCell("dolt", "a-key", 30, null);
        validateSizes(5, 1);

        warden.returnCell("dolt");
        validateSizes(6, 0);
    }

    private void validateSizes(int available, int reserved) {
        assertEquals("incorrect number of available cells", available,
                     warden.getAvailableCells().size());
        assertEquals("incorrect number of reserved cells", reserved,
                     warden.getReservedCells().size());
    }

    private void validateCellState(Reservation reservation) {
        assertFalse("cell should not be available",
                    warden.getAvailableCells().contains(reservation.cellName));
        assertTrue("cell should be reserved",
                   warden.getReservedCells().contains(reservation.cellName));
    }

}