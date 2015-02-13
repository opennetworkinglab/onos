/*
 * Copyright 2014,2015 Open Networking Laboratory
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
 ONOS GUI -- Util -- Key Handler Service

 @author Simon Hunt
 */
(function () {
    'use strict';

    // references to injected services
    var $log, fs, ts;

    // internal state
    var keyHandler = {
            globalKeys: {},
            maskedKeys: {},
            viewKeys: {},
            viewFn: null,
            viewGestures: []
        };

    // TODO: we need to have the concept of view token here..
    function getViewToken() {
        return 'NotYetAViewToken';
    }

    function whatKey(code) {
        switch (code) {
            case 13: return 'enter';
            case 16: return 'shift';
            case 17: return 'ctrl';
            case 18: return 'alt';
            case 27: return 'esc';
            case 32: return 'space';
            case 37: return 'leftArrow';
            case 38: return 'upArrow';
            case 39: return 'rightArrow';
            case 40: return 'downArrow';
            case 91: return 'cmdLeft';
            case 93: return 'cmdRight';
            case 187: return 'equals';
            case 189: return 'dash';
            case 191: return 'slash';
            case 192: return 'backQuote';
            case 220: return 'backSlash';
            default:
                if ((code >= 48 && code <= 57) ||
                    (code >= 65 && code <= 90)) {
                    return String.fromCharCode(code);
                } else if (code >= 112 && code <= 123) {
                    return 'F' + (code - 111);
                }
                return '.';
        }
    }

    function keyIn() {
        var event = d3.event,
            keyCode = event.keyCode,
            key = whatKey(keyCode),
            kh = keyHandler,
            gk = kh.globalKeys[key],
            gcb = fs.isF(gk) || (fs.isA(gk) && fs.isF(gk[0])),
            vk = kh.viewKeys[key],
            vcb = fs.isF(vk) || (fs.isA(vk) && fs.isF(vk[0])) || fs.isF(kh.viewFn),
            token = getViewToken();

        d3.event.stopPropagation();

        // global callback?
        if (gcb && gcb(token, key, keyCode, event)) {
            // if the event was 'handled', we are done
            return;
        }
        // otherwise, let the view callback have a shot
        if (vcb) {
            vcb(token, key, keyCode, event);
        }
    }

    function setupGlobalKeys() {
        $.extend(keyHandler, {
            globalKeys: {
                backSlash: [quickHelp, 'Show / hide Quick Help'],
                slash: [quickHelp, 'Show / hide Quick Help'],
                esc: [escapeKey, 'Dismiss dialog or cancel selections'],
                T: [toggleTheme, "Toggle theme"]
            },
            globalFormat: ['backSlash', 'slash', 'esc', 'T'],

            // Masked keys are global key handlers that always return true.
            // That is, the view will never see the event for that key.
            maskedKeys: {
                slash: true,
                backSlash: true,
                T: true
            }
        });
    }

    function quickHelp(view, key, code, ev) {
        // TODO: show quick help
        // delegate to QuickHelp service.
        //libApi.quickHelp.show(keyHandler);
        console.log('QUICK-HELP');
        return true;
    }

    function escapeKey(view, key, code, ev) {
        // TODO: plumb in handling of alerts and quick help dismissal
        // We will delegate to the Alert / QuickHelp Services as appropriate.
/*
        if (alerts.open) {
            closeAlerts();
            return true;
        }
        if (libApi.quickHelp.hide()) {
            return true;
        }
*/
        console.log('ESCAPE');
        return false;
    }

    function toggleTheme(view, key, code, ev) {
        ts.toggleTheme();
        return true;
    }

    function setKeyBindings(keyArg) {
        var viewKeys,
            masked = [];

        if (fs.isF(keyArg)) {
            // set general key handler callback
            keyHandler.viewFn = keyArg;
        } else {
            // set specific key filter map
            viewKeys = d3.map(keyArg).keys();
            viewKeys.forEach(function (key) {
                if (keyHandler.maskedKeys[key]) {
                    masked.push('setKeyBindings(): Key "' + key + '" is reserved');
                }
            });

            if (masked.length) {
                $log.warn(masked.join('\n'));
            }
            keyHandler.viewKeys = keyArg;
        }
    }

    function getKeyBindings() {
        var gkeys = d3.map(keyHandler.globalKeys).keys(),
            masked = d3.map(keyHandler.maskedKeys).keys(),
            vkeys = d3.map(keyHandler.viewKeys).keys(),
            vfn = !!fs.isF(keyHandler.viewFn);

        return {
            globalKeys: gkeys,
            maskedKeys: masked,
            viewKeys: vkeys,
            viewFunction: vfn
        };
    }

    angular.module('onosUtil')
        .factory('KeyService', ['$log', 'FnService', 'ThemeService',
        function (_$log_, _fs_, _ts_) {
            $log = _$log_;
            fs = _fs_;
            ts = _ts_;

            return {
                installOn: function (elem) {
                    elem.on('keydown', keyIn);
                    setupGlobalKeys();
                },
                keyBindings: function (x) {
                    if (x === undefined) {
                        return getKeyBindings();
                    } else {
                        setKeyBindings(x);
                    }
                },
                gestureNotes: function (g) {
                    if (g === undefined) {
                        return keyHandler.viewGestures;
                    } else {
                        keyHandler.viewGestures = fs.isA(g) || [];
                    }
                }
            };
    }]);

}());
