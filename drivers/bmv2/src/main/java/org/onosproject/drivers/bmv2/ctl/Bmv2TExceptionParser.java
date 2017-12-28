/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 */

package org.onosproject.drivers.bmv2.ctl;


import org.apache.thrift.TException;
import org.onosproject.bmv2.thriftapi.InvalidMcOperation;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2RuntimeException;

import static org.onosproject.drivers.bmv2.api.runtime.Bmv2RuntimeException.Code;

/**
 * Utility class to translate a Thrift exception into a Bmv2RuntimeException.
 */
final class Bmv2TExceptionParser {

    private Bmv2TExceptionParser() {
        // ban constructor.
    }

    static Bmv2RuntimeException parseTException(TException cause) {
        try {
            return new Bmv2RuntimeException(getCode(cause));
        } catch (ParserException e) {
            return new Bmv2RuntimeException(e.codeString);
        }
    }

    private static Code getCode(TException e) throws ParserException {
        if (e instanceof InvalidMcOperation) {
            switch (((InvalidMcOperation) e).getCode()) {
                case TABLE_FULL:
                    return Code.TABLE_FULL;
                case INVALID_MGID:
                    return Code.INVALID_MGID;
                case INVALID_L1_HANDLE:
                    return Code.INVALID_L1_HANDLE;
                case INVALID_MGRP_HANDLE:
                    return Code.INVALID_MGRP_HANDLE;
                case ERROR:
                    return Code.MC_GENERAL_ERROR;
                default:
                    return Code.MC_UNKNOWN_ERROR;
            }
        } else {
            throw new ParserException(e.toString());
        }
    }

    private static class ParserException extends Exception {

        private String codeString;

        public ParserException(String codeString) {
            this.codeString = codeString;
        }
    }
}
