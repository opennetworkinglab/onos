/*
 * Copyright 2015-present Open Networking Laboratory
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

/*
 Module containing the "business logic" for the Path Painter topology overlay.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, wss;

    // constants
    var srcMessage = 'ppTopovSetSrc',
        dstMessage = 'ppTopovSetDst',
        swapMessage = 'ppTopovSwapSrcDst',
        modeMessage = 'ppTopovSetMode',
        nextPathMessage = 'ppTopovNextPath',
        clearMessage = 'ppTopovClear',
        prevPathMessage = 'ppTopovPrevPath';

    // internal state
    var currentMode = null;
    var selected = false;


    // === ---------------------------
    // === Helper functions


    // === ---------------------------
    // === Main API functions

    function clear() {
        if (selected) {
            selected = false;
            wss.sendEvent(clearMessage);
            flash.flash('Cleared source and destination');
            return true;
        }
        return false;
    }

    function setSrc(node) {
        selected = true;
        wss.sendEvent(srcMessage, {
            id: node.id,
            type: node.type
        });
        flash.flash('Source node: ' + node.id);
    }

    function setDst(node) {
        selected = true;
        wss.sendEvent(dstMessage, {
            id: node.id,
            type: node.type
        });
        flash.flash('Destination node: ' + node.id);
    }

    function swapSrcDst() {
        wss.sendEvent(swapMessage)
        flash.flash('Source and destination swap');
    }

    function nextPath() {
        wss.sendEvent(nextPathMessage);
    }

    function prevPath() {
        wss.sendEvent(prevPathMessage);
    }

    function setMode(mode) {
        if (currentMode === mode) {
            $log.debug('(in mode', mode, 'already)');
            flash.flash('Already in ' + mode + ' mode');
        } else {
            currentMode = mode;
            wss.sendEvent(modeMessage, {
                mode: mode
            });
            flash.flash('Path mode: ' + mode);
        }
    }

    // === ---------------------------
    // === Module Factory Definition

    angular.module('ovPpTopov', [])
        .factory('PathPainterTopovService',
        ['$log', 'FnService', 'FlashService', 'WebSocketService',

        function (_$log_, _fs_, _flash_, _wss_) {
            $log = _$log_;
            fs = _fs_;
            flash = _flash_;
            wss = _wss_;

            return {
                setSrc: setSrc,
                setDst: setDst,
                setMode: setMode,
                nextPath: nextPath,
                prevPath: prevPath,
                swapSrcDst: swapSrcDst,
                clear: clear
            };
        }]);
}());
