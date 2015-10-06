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
 */
(function () {
    'use strict';

    // references to injected services
    var $log, fs, ts, ns, qhs;

    // internal state
    var enabled = true,
        globalEnabled = true,
        keyHandler = {
            globalKeys: {},
            maskedKeys: {},
            viewKeys: {},
            viewFn: null,
            viewGestures: []
        };

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
            case 188: return 'comma';
            case 189: return 'dash';
            case 190: return 'dot';
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
            kl = fs.isF(kh.viewKeys._keyListener),
            vcb = fs.isF(vk) || (fs.isA(vk) && fs.isF(vk[0])) || fs.isF(kh.viewFn),
            token = 'keyev';    // indicate this was a key-pressed event

        d3.event.stopPropagation();

        if (enabled) {
            // global callback?
            if (gcb && gcb(token, key, keyCode, event)) {
                // if the event was 'handled', we are done
                return;
            }
            // otherwise, let the view callback have a shot
            if (vcb) {
                vcb(token, key, keyCode, event);
            }
            if (kl) {
                kl(key);
            }
        }
    }

    function setupGlobalKeys() {
        angular.extend(keyHandler, {
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
        if (!globalEnabled) {
            return false;
        }
        qhs.showQuickHelp(keyHandler);
        return true;
    }

    // returns true if we 'consumed' the ESC keypress, false otherwise
    function escapeKey(view, key, code, ev) {
        return ns.hideIfShown() || qhs.hideQuickHelp();
    }

    function toggleTheme(view, key, code, ev) {
        if (!globalEnabled) {
            return false;
        }
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

    function unbindKeys() {
        keyHandler.viewKeys = {};
        keyHandler.viewFn = null;
        keyHandler.viewGestures = [];
    }

    function checkNotGlobal(o) {
        var oops = [];
        if (fs.isO(o)) {
            angular.forEach(o, function (val, key) {
                if (keyHandler.globalKeys[key]) {
                    oops.push(key);
                }
            });
            if (oops.length) {
                $log.warn('Ignoring reserved global key(s):', oops.join(','));
                oops.forEach(function (key) {
                    delete o[key];
                });
            }
        }
    }

    angular.module('onosUtil')
    .factory('KeyService',
        ['$log', 'FnService', 'ThemeService', 'NavService',

        function (_$log_, _fs_, _ts_, _ns_) {
            $log = _$log_;
            fs = _fs_;
            ts = _ts_;
            ns = _ns_;

            return {
                bindQhs: function (_qhs_) {
                    qhs = _qhs_;
                },
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
                unbindKeys: unbindKeys,
                gestureNotes: function (g) {
                    if (g === undefined) {
                        return keyHandler.viewGestures;
                    } else {
                        keyHandler.viewGestures = fs.isA(g) || [];
                    }
                },
                enableKeys: function (b) {
                    enabled = b;
                },
                enableGlobalKeys: function (b) {
                    globalEnabled = b;
                },
                checkNotGlobal: checkNotGlobal
            };
    }]);

}());
