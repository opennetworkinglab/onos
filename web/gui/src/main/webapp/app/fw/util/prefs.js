/*
 * Copyright 2015-present Open Networking Foundation
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
 ONOS GUI -- Util -- User Preference Service
 */
(function () {
    'use strict';

    // injected refs
    var fs, wss;

    // internal state
    var cache = {},
        listeners = [];

    // returns the preference settings for the specified key
    function getPrefs(name, defaults, qparams) {
        var obj = angular.extend({}, defaults || {}, cache[name] || {});

        // if query params are specified, they override...
        if (fs.isO(qparams)) {
            angular.forEach(obj, function (v, k) {
                if (qparams.hasOwnProperty(k)) {
                    obj[k] = qparams[k];
                }
            });
        }
        return obj;
    }

    // converts string values to numbers for selected (or all) keys
    // asNumbers(obj, ['a', 'b'])        <-- convert keys .a, .b to numbers
    // asNumbers(obj, ['a', 'b'], true)  <-- convert ALL BUT keys .a, .b to numbers

    function asNumbers(obj, keys, not) {
        if (!obj) return null;

        var skip = {};
        if (not) {
            keys.forEach(function (k) {
                skip[k] = 1;
            });
        }

        if (!keys || not) {
            // do them all
            angular.forEach(obj, function (v, k) {
                if (!not || !skip[k]) {
                    obj[k] = Number(obj[k]);
                }
            });
        } else {
            // do the explicitly named keys
            keys.forEach(function (k) {
                obj[k] = Number(obj[k]);
            });
        }
        return obj;
    }

    function setPrefs(name, obj) {
        // keep a cached copy of the object and send an update to server
        cache[name] = obj;
        wss.sendEvent('updatePrefReq', { key: name, value: obj });
    }

    // merge preferences:
    // The assumption here is that obj is a sparse object, and that the
    //  defined keys should overwrite the corresponding values, but any
    //  existing keys that are NOT explicitly defined here should be left
    //  alone (not deleted).
    function mergePrefs(name, obj) {
        var merged = cache[name] || {};
        setPrefs(name, angular.extend(merged, obj));
    }

    function updatePrefs(data) {
        cache = data;
        listeners.forEach(function (lsnr) { lsnr(); });
    }

    function addListener(listener) {
        listeners.push(listener);
    }

    function removeListener(listener) {
        listeners = listeners.filter(function (obj) { return obj === listener; });
    }

    angular.module('onosUtil')
    .factory('PrefsService', ['FnService', 'WebSocketService',
        function (_fs_, _wss_) {
            fs = _fs_;
            wss = _wss_;

            try {
                cache = angular.isDefined(userPrefs) ? userPrefs : {};
            }
            catch (e) {
                // browser throws error for non-existing globals
                cache = {};
            }

            wss.bindHandlers({
                updatePrefs: updatePrefs,
            });

            return {
                getPrefs: getPrefs,
                asNumbers: asNumbers,
                setPrefs: setPrefs,
                mergePrefs: mergePrefs,
                addListener: addListener,
                removeListener: removeListener,
            };
        }]);

}());
