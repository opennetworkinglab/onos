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

package org.onosproject.bmv2.ctl;


import org.apache.thrift.TException;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.thriftapi.InvalidCounterOperation;
import org.onosproject.bmv2.thriftapi.InvalidDevMgrOperation;
import org.onosproject.bmv2.thriftapi.InvalidLearnOperation;
import org.onosproject.bmv2.thriftapi.InvalidMcOperation;
import org.onosproject.bmv2.thriftapi.InvalidMeterOperation;
import org.onosproject.bmv2.thriftapi.InvalidRegisterOperation;
import org.onosproject.bmv2.thriftapi.InvalidSwapOperation;
import org.onosproject.bmv2.thriftapi.InvalidTableOperation;

import static org.onosproject.bmv2.api.runtime.Bmv2RuntimeException.Code;

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
        if (e instanceof InvalidTableOperation) {
            InvalidTableOperation t = (InvalidTableOperation) e;
            switch (t.getCode()) {
                case TABLE_FULL:
                    return Code.TABLE_FULL;
                case INVALID_HANDLE:
                    return Code.TABLE_INVALID_HANDLE;
                case EXPIRED_HANDLE:
                    return Code.TABLE_EXPIRED_HANDLE;
                case COUNTERS_DISABLED:
                    return Code.TABLE_COUNTERS_DISABLED;
                case METERS_DISABLED:
                    return Code.TABLE_METERS_DISABLED;
                case AGEING_DISABLED:
                    return Code.TABLE_AGEING_DISABLED;
                case INVALID_TABLE_NAME:
                    return Code.TABLE_INVALID_TABLE_NAME;
                case INVALID_ACTION_NAME:
                    return Code.TABLE_INVALID_ACTION_NAME;
                case WRONG_TABLE_TYPE:
                    return Code.TABLE_WRONG_TABLE_TYPE;
                case INVALID_MBR_HANDLE:
                    return Code.TABLE_INVALID_MBR_HANDLE;
                case MBR_STILL_USED:
                    return Code.TABLE_MBR_STILL_USED;
                case MBR_ALREADY_IN_GRP:
                    return Code.TABLE_MBR_ALREADY_IN_GRP;
                case MBR_NOT_IN_GRP:
                    return Code.TABLE_MBR_NOT_IN_GRP;
                case INVALID_GRP_HANDLE:
                    return Code.TABLE_INVALID_GRP_HANDLE;
                case GRP_STILL_USED:
                    return Code.TABLE_GRP_STILL_USED;
                case EMPTY_GRP:
                    return Code.TABLE_EMPTY_GRP;
                case DUPLICATE_ENTRY:
                    return Code.TABLE_DUPLICATE_ENTRY;
                case BAD_MATCH_KEY:
                    return Code.TABLE_BAD_MATCH_KEY;
                case INVALID_METER_OPERATION:
                    return Code.TABLE_INVALID_METER_OPERATION;
                case DEFAULT_ACTION_IS_CONST:
                    return Code.TABLE_DEFAULT_ACTION_IS_CONST;
                case DEFAULT_ENTRY_IS_CONST:
                    return Code.TABLE_DEFAULT_ENTRY_IS_CONST;
                case ERROR:
                    return Code.TABLE_GENERAL_ERROR;
                default:
                    return Code.TABLE_UNKNOWN_ERROR;
            }
        } else if (e instanceof InvalidCounterOperation) {
            InvalidCounterOperation t = (InvalidCounterOperation) e;
            switch (t.getCode()) {
                case INVALID_COUNTER_NAME:
                    return Code.COUNTER_INVALID_NAME;
                case INVALID_INDEX:
                    return Code.COUNTER_INVALID_INDEX;
                case ERROR:
                    return Code.COUNTER_ERROR_GENERAL;
                default:
                    return Code.COUNTER_ERROR_UNKNOWN;
            }
        } else if (e instanceof InvalidDevMgrOperation) {
            InvalidDevMgrOperation t = (InvalidDevMgrOperation) e;
            switch (t.getCode()) {
                case ERROR:
                    return Code.DEV_MGR_ERROR_GENERAL;
                default:
                    return Code.DEV_MGR_UNKNOWN;
            }
        } else if (e instanceof InvalidSwapOperation) {
            InvalidSwapOperation t = (InvalidSwapOperation) e;
            switch (t.getCode()) {
                case CONFIG_SWAP_DISABLED:
                    return Code.SWAP_CONFIG_DISABLED;
                case ONGOING_SWAP:
                    return Code.SWAP_ONGOING;
                case NO_ONGOING_SWAP:
                    return Code.SWAP_NO_ONGOING;
                default:
                    return Code.SWAP_ERROR_UKNOWN;
            }
        } else if (e instanceof InvalidMeterOperation) {
            // TODO
            throw new ParserException(e.toString());
        } else if (e instanceof InvalidRegisterOperation) {
            // TODO
            throw new ParserException(e.toString());
        } else if (e instanceof InvalidLearnOperation) {
            // TODO
            throw new ParserException(e.toString());
        } else if (e instanceof InvalidMcOperation) {
            // TODO
            throw new ParserException(e.toString());
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
