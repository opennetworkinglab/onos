/*
 * Copyright 2014-present Open Networking Laboratory
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
    var $log, $timeout, fs, ts, ns, ee, qhs;

    // internal state
    var enabled = true,
        globalEnabled = true,
        keyHandler = {
            globalKeys: {},
            maskedKeys: {},
            dialogKeys: {},
            viewKeys: {},
            viewFn: null,
            viewGestures: []
        },
        seq = {},
        matching = false,
        matched = '',
        lookup;

    function matchSeq(key) {
        if (!matching && key === 'shift-shift') {
            matching = true;
            return true;
        }
        if (matching) {
            matched += key;
            lookup = fs.trieLookup(seq, matched);
            if (lookup === -1) {
                return true;
            }
            matching = false;
            matched = '';
            if (!lookup) {
                return;
            }
            ee.cluck(lookup);
            return true;
        }
    }

    function whatKey(code) {
        switch (code) {
            case 8: return 'delete';
            case 9: return 'tab';
            case 13: return 'enter';
            case 16: return 'shift';
            case 27: return 'esc';
            case 32: return 'space';
            case 37: return 'leftArrow';
            case 38: return 'upArrow';
            case 39: return 'rightArrow';
            case 40: return 'downArrow';
            case 186: return 'semicolon';
            case 187: return 'equals';
            case 188: return 'comma';
            case 189: return 'dash';
            case 190: return 'dot';
            case 191: return 'slash';
            case 192: return 'backQuote';
            case 219: return 'openBracket';
            case 220: return 'backSlash';
            case 221: return 'closeBracket';
            case 222: return 'quote';
            default:
                if ((code >= 48 && code <= 57) ||
                    (code >= 65 && code <= 90)) {
                    return String.fromCharCode(code);
                } else if (code >= 112 && code <= 123) {
                    return 'F' + (code - 111);
                }
                return null;
        }
    }

    var textFieldDoesNotBlock = {
        enter: 1,
        esc: 1
    };

    function textFieldInput() {
        var t = d3.event.target.tagName.toLowerCase();
        return t === 'input' || t === 'textarea';
    }

    function keyIn() {
        var event = d3.event,
            keyCode = event.keyCode,
            key = whatKey(keyCode),
            textBlockable = !textFieldDoesNotBlock[key],
            modifiers = [];

        event.metaKey && modifiers.push('cmd');
        event.altKey && modifiers.push('alt');
        event.shiftKey && modifiers.push('shift');

        if (!key) {
            return;
        }

        modifiers.push(key);
        key = modifiers.join('-');

        if (textBlockable && textFieldInput()) {
            return;
        }

        var kh = keyHandler,
            gk = kh.globalKeys[key],
            gcb = fs.isF(gk) || (fs.isA(gk) && fs.isF(gk[0])),
            dk = kh.dialogKeys[key],
            dcb = fs.isF(dk),
            vk = kh.viewKeys[key],
            kl = fs.isF(kh.viewKeys._keyListener),
            vcb = fs.isF(vk) || (fs.isA(vk) && fs.isF(vk[0])) || fs.isF(kh.viewFn),
            token = 'keyev';    // indicate this was a key-pressed event

        event.stopPropagation();

        if (enabled) {
            if (matchSeq(key)) return;

            // global callback?
            if (gcb && gcb(token, key, keyCode, event)) {
                // if the event was 'handled', we are done
                return;
            }
            // dialog callback?
            if (dcb) {
                dcb(token, key, keyCode, event);
                // assume dialog handled the event
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
                slash: 1,
                backSlash: 1,
                T: 1
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

    function filterMaskedKeys(map, caller, remove) {
        var masked = [],
            msgs = [];

        d3.map(map).keys().forEach(function (key) {
            if (keyHandler.maskedKeys[key]) {
                masked.push(key);
                msgs.push(caller, ': Key "' + key + '" is reserved');
            }
        });

        if (msgs.length) {
            $log.warn(msgs.join('\n'));
        }

        if (remove) {
            masked.forEach(function (k) {
                delete map[k];
            });
        }
        return masked;
    }

    function unexParam(fname, x) {
        $log.warn(fname, ": unexpected parameter-- ", x);
    }

    function setKeyBindings(keyArg) {
        var fname = 'setKeyBindings()',
            kFunc = fs.isF(keyArg),
            kMap = fs.isO(keyArg);

        if (kFunc) {
            // set general key handler callback
            keyHandler.viewFn = kFunc;
        } else if (kMap) {
            filterMaskedKeys(kMap, fname, true);
            keyHandler.viewKeys = kMap;
        } else {
            unexParam(fname, keyArg);
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

    function bindDialogKeys(map) {
        var fname = 'bindDialogKeys()',
            kMap = fs.isO(map);

        if (kMap) {
            filterMaskedKeys(map, fname, true);
            keyHandler.dialogKeys = kMap;
        } else {
            unexParam(fname, map);
        }
    }

    function unbindDialogKeys() {
        keyHandler.dialogKeys = {};
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
        ['$log', '$timeout', 'FnService', 'ThemeService', 'NavService',
            'EeService',

        function (_$log_, _$timeout_, _fs_, _ts_, _ns_, _ee_) {
            $log = _$log_;
            $timeout = _$timeout_;
            fs = _fs_;
            ts = _ts_;
            ns = _ns_;
            ee = _ee_;

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
                dialogKeys: function (x) {
                    if (x === undefined) {
                        unbindDialogKeys();
                    } else {
                        bindDialogKeys(x);
                    }
                },
                addSeq: function (word, data) {
                    fs.addToTrie(seq, word, data);
                },
                remSeq: function (word) {
                    fs.removeFromTrie(seq, word);
                },
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
