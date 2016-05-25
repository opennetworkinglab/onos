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

package org.onosproject.drivers.cisco;

import com.google.common.collect.Lists;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;

import java.util.Arrays;
import java.util.List;

import static org.onosproject.net.Port.Type;

/**
 *Parser for Netconf configurations and replys as plain text.
 */
public final class TextBlockParserCisco {

    private static final String BANDWIDTH = "BW ";
    private static final String SPEED = " Kbit/sec";
    private static final String ETHERNET = "Eth";
    private static final String FASTETHERNET = "Fas";
    private static final String GIGABITETHERNET = "Gig";
    private static final String FDDI = "Fdd";
    private static final String POS = "POS";
    private static final String SERIAL = "Ser";
    private static final List INTERFACES = Arrays.asList(ETHERNET, FASTETHERNET, GIGABITETHERNET, SERIAL, FDDI, POS);
    private static final List FIBERINTERFACES = Arrays.asList(FDDI, POS);

    private static final String NEWLINE_SPLITTER = "\n";
    private static final String PORT_DELIMITER = "/";
    private static final String SPACE = " ";
    private static final String IS_UP = "is up, line protocol is up";


    private TextBlockParserCisco() {
        //not called, preventing any allocation
    }

    /**
     * Calls methods to create information about Ports.
     * @param interfacesReply the interfaces as plain text
     * @return the Port description list
     */
    public static List<PortDescription> parseCiscoIosPorts(String interfacesReply) {
        String parentInterface;
        String[] parentArray;
        String tempString;
        List<PortDescription> portDesc = Lists.newArrayList();
        int interfacesCounter = interfacesCounterMethod(interfacesReply);
        for (int i = 0; i < interfacesCounter; i++) {
            parentInterface = parentInterfaceMethod(interfacesReply);
            portDesc.add(findPortInfo(parentInterface));
            parentArray = parentInterface.split(SPACE);
            tempString = parentArray[0] + SPACE;
            interfacesReply = interfacesReply.replace(tempString, SPACE + tempString);
        }
        return portDesc;
    }

    /**
     * Creates the port information object.
     * @param interfaceTree the interfaces as plain text
     * @return the Port description object
     */
    private static DefaultPortDescription findPortInfo(String interfaceTree) {
        String[] textStr = interfaceTree.split(NEWLINE_SPLITTER);
        String[] firstLine = textStr[0].split(SPACE);
        String firstWord = firstLine[0];
        Type type = getPortType(textStr);
        boolean isEnabled = getIsEnabled(textStr);
        String port = getPort(textStr);
        long portSpeed = getPortSpeed(textStr);
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, firstWord);
        return port == "-1" ? null : new DefaultPortDescription(PortNumber.portNumber(port),
                                                                isEnabled, type, portSpeed, annotations.build());
    }

    /**
     * Counts the number of existing interfaces.
     * @param interfacesReply the interfaces as plain text
     * @return interfaces counter
     */
    private static int interfacesCounterMethod(String interfacesReply) {
        int counter;
        String first3Characters;
        String[] textStr = interfacesReply.split(NEWLINE_SPLITTER);
        int lastLine = textStr.length - 1;
        counter = 0;
        for (int i = 1; i < lastLine; i++) {
            first3Characters = textStr[i].substring(0, 3);
            if (INTERFACES.contains(first3Characters)) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Parses the text and seperates to Parent Interfaces.
     * @param interfacesReply the interfaces as plain text
     * @return Parent interface
     */
    private static String parentInterfaceMethod(String interfacesReply) {
        String firstCharacter;
        String first3Characters;
        boolean isChild = false;
        StringBuilder anInterface = new StringBuilder("");
        String[] textStr = interfacesReply.split("\\n");
        int lastLine = textStr.length - 1;
        for (int i = 1; i < lastLine; i++) {
            firstCharacter = textStr[i].substring(0, 1);
            first3Characters = textStr[i].substring(0, 3);
            if (!(firstCharacter.equals(SPACE)) && isChild) {
                break;
            } else if (firstCharacter.equals(SPACE) && isChild) {
                anInterface.append(textStr[i] + NEWLINE_SPLITTER);
            } else if (INTERFACES.contains(first3Characters)) {
                isChild = true;
                anInterface.append(textStr[i] + NEWLINE_SPLITTER);
            }
        }
        return anInterface.toString();
    }

    /**
     * Get the port type for an interface.
     * @param textStr interface splitted as an array
     * @return Port type
     */
    private static Type getPortType(String[] textStr) {
        String first3Characters;
        first3Characters = textStr[0].substring(0, 3);
        return FIBERINTERFACES.contains(first3Characters) ? Type.FIBER : Type.COPPER;
    }

    /**
     * Get the state for an interface.
     * @param textStr interface splitted as an array
     * @return isEnabled state
     */
    private static boolean getIsEnabled(String[] textStr) {
        return textStr[0].contains(IS_UP);
    }

    /**
     * Get the port number for an interface.
     * @param textStr interface splitted as an array
     * @return port number
     */
    private static String getPort(String[] textStr) {
        String port;
        try {
            if (textStr[0].indexOf(PORT_DELIMITER) > 0) {
                port = textStr[0].substring(textStr[0].lastIndexOf(PORT_DELIMITER) + 1,
                                            textStr[0].indexOf(SPACE));
            } else {
                port = "-1";
            }
        } catch (RuntimeException e) {
            port = "-1";
        }
        return port;
    }

    /**
     * Get the port speed for an interface.
     * @param textStr interface splitted as an array
     * @return port speed
     */
    private static long getPortSpeed(String[] textStr) {
        long portSpeed = 0;
        String result;
        int lastLine = textStr.length - 1;
        for (int i = 0; i < lastLine; i++) {
            if ((textStr[i].indexOf(BANDWIDTH) > 0) && (textStr[i].indexOf(SPEED) > 0)) {
                result = textStr[i].substring(textStr[i].indexOf(BANDWIDTH) + 3, textStr[i].indexOf(SPEED));
                portSpeed = Long.valueOf(result);
                break;
            }
        }
        return portSpeed;
    }

}
