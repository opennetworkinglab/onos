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
 ONOS GUI -- Remote Communications Module -- REST Service
 */
(function () {
    'use strict';

    var $log;

    angular.module('onosRemote')
    .factory('RestService',
        ['$log', '$http', 'FnService', 'UrlFnService',

        function (_$log_, $http, fs, ufs) {
            $log = _$log_;

            function get(url, callback, errorCb) {
                var fullUrl = ufs.rsUrl(url);

                $http.get(fullUrl).then(function (response) {
                    // success
                    callback(response.data);
                }, function (response) {
                    // error
                    var emsg = 'Failed to retrieve JSON data: ' + fullUrl;
                    $log.warn(emsg, response.status, response.data);
                    if (errorCb) {
                        errorCb(emsg);
                    }
                });
            }

            // TODO: test this
            function post(url, data, callbacks) {
                var fullUrl = ufs.rsUrl(url);
                $http.post(fullUrl, data).then(function (response) {
                    // success
                    if (callbacks && fs.isF(callbacks.success)) {
                        callbacks.success(response.data);
                    }
                }, function (response) {
                    // error
                    var msg = 'Problem with $http post request: ' + fullUrl;
                    $log.warn(msg, response.status, response.data);

                    if (callbacks && fs.isF(callbacks.error)) {
                        callbacks.error(msg);
                    }
                });
            }

            return {
                get: get,
                post: post,
            };
        }]);
}());
