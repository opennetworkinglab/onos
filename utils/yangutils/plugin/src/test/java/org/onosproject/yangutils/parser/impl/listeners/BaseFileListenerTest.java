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

package org.onosproject.yangutils.parser.impl.listeners;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

/**
 * Test cases for testing base rule listener functionality.
 */
public class BaseFileListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Checks for exception if stack of parsable data is not empty at the entry
     * of yang base rule.
     */
    @Test
    public void processYangFileEntryNonEmptyStack() {
        thrown.expect(ParserException.class);
        thrown.expectMessage("Internal parser error detected: Invalid holder for yangbase before processing.");

        YangModule tmpModule = new YangModule();
        TreeWalkListener listener = new TreeWalkListener();
        listener.getParsedDataStack().push(tmpModule);
        GeneratedYangParser.YangfileContext ctx = null;
        BaseFileListener.processYangFileEntry(listener, ctx);
    }

    /**
     * Checks that exception shouldn't be generated if stack of parsable data is
     * empty at the entry of yang base rule.
     */
    @Test
    public void processYangFileEntryEmptyStack() {

        TreeWalkListener listener = new TreeWalkListener();
        GeneratedYangParser.YangfileContext ctx = null;
        BaseFileListener.processYangFileEntry(listener, ctx);
    }

    /**
     * Checks that exception should be generated if stack of parsable data is
     * not empty at the exit of yang base rule.
     */
    @Test
    public void processYangFileExitEmptyStack() {
        thrown.expect(ParserException.class);
        thrown.expectMessage("Internal parser error detected: Missing holder at yangbase after processing.");

        TreeWalkListener listener = new TreeWalkListener();
        GeneratedYangParser.YangfileContext ctx = null;
        BaseFileListener.processYangFileExit(listener, ctx);
    }

    /**
     * Checks that exception shouldn't be generated if stack of parsable data is
     * empty at the exit of yang base rule.
     */
    @Test
    public void processYangFileExitNonEmptyStack() {

        TreeWalkListener listener = new TreeWalkListener();
        GeneratedYangParser.YangfileContext ctx = null;
        BaseFileListener.processYangFileEntry(listener, ctx);
    }

    /**
     * Checks that after popping out the parsable node from stack it should be
     * empty.
     */
    @Test
    public void processYangFileExitStackErrorExtraEntryTest() {
        thrown.expect(ParserException.class);
        thrown.expectMessage("Internal parser error detected: Invalid holder for yangbase after processing.");

        YangModule tmpModule = new YangModule();
        YangModule tmpModule2 = new YangModule();
        TreeWalkListener listener = new TreeWalkListener();
        listener.getParsedDataStack().push(tmpModule);
        listener.getParsedDataStack().push(tmpModule2);
        GeneratedYangParser.YangfileContext ctx = null;
        BaseFileListener.processYangFileExit(listener, ctx);
    }
}