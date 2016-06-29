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
 ONOS GUI -- Util -- User Preference Service
 */
(function () {
    'use strict';

    // injected refs
    var $log, fs, wss;

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
    function asNumbers(obj, keys) {
        if (!obj) return null;

        if (!keys) {
            // do them all
            angular.forEach(obj, function (v, k) {
                obj[k] = Number(obj[k]);
            });
        } else {
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
    
    function updatePrefs(data) {
        $log.info('User properties updated');
        cache = data;
        listeners.forEach(function (lsnr) { lsnr(); });
    }

    function addListener(listener) {
        listeners.push(listener);
    }

    function removeListener(listener) {
        listeners = listeners.filter(function(obj) { return obj === listener; });
    }

    angular.module('onosUtil')
    .factory('PrefsService', ['$log', 'FnService', 'WebSocketService',
        function (_$log_, _fs_, _wss_) {
            $log = _$log_;
            fs = _fs_;
            wss = _wss_;

            try {
                cache = angular.isDefined(userPrefs) ? userPrefs : {};
            }
            catch(e){
                // browser throws error for non-existing globals
                cache = {}
            }

            wss.bindHandlers({
                updatePrefs: updatePrefs
            });

            return {
                getPrefs: getPrefs,
                asNumbers: asNumbers,
                setPrefs: setPrefs,
                addListener: addListener,
                removeListener: removeListener
            };
        }]);

}());
