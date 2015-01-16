/*
 * Copyright 2015 Open Networking Laboratory
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

 @author Bri Prebilic Cole
 @author Simon Hunt
 */
(function () {
    'use strict';

    var $log;

    var urlSuffix = '/ui/rs/';



    // TODO: remove temporary test code
    var fakeData = {
        '1': {
            "devices": [{
                "id": "of:0000000000000001",
                "available": true,
                "role": "MASTER",
                "mfr": "Nicira, Inc.",
                "hw": "Open vSwitch",
                "sw": "2.0.1",
                "serial": "None",
                "annotations": {
                    "protocol": "OF_10"
                }
            },
                {
                    "id": "of:0000000000000004",
                    "available": true,
                    "role": "MASTER",
                    "mfr": "Nicira, Inc.",
                    "hw": "Open vSwitch",
                    "sw": "2.0.1",
                    "serial": "None",
                    "annotations": {
                        "protocol": "OF_10"
                    }
                }]
        },
        '2': {
            "devices": [{
                "id": "of:0000000000000002",
                "available": true,
                "role": "MASTER",
                "mfr": "Nicira, Inc.",
                "hw": "Open vSwitch",
                "sw": "2.0.0",
                "serial": "None",
                "annotations": {
                    "protocol": "OF_10"
                }
            },
                {
                    "id": "of:0000000000000006",
                    "available": true,
                    "role": "MASTER",
                    "mfr": "Nicira, Inc.",
                    "hw": "Open vSwitch",
                    "sw": "2.1.1",
                    "serial": "None",
                    "annotations": {
                        "protocol": "OF_10"
                    }
                }]
        },
        'empty': {
            devices: []
        }
    };

    function getFakeData(url) {
        var id = url.slice(5);

        return fakeData[id] || fakeData.empty;
    }

    angular.module('onosRemote')
        .factory('RestService', ['$log', '$http', 'UrlFnService',
        function (_$log_, $http, ufs) {
            $log = _$log_;

            function get(url, callback) {
                // TODO: remove temporary test code
                if (url.match(/^test\//)) {
                    callback(getFakeData(url));
                    return;
                }
                var fullUrl = ufs.urlPrefix() + urlSuffix + url;

                $http.get(fullUrl).then(function (response) {
                    // success
                    callback(response.data);
                }, function (response) {
                    // error
                    $log.warn('Failed to retrieve JSON data: ' + fullUrl,
                        response.status, response.data);
                });
            }

            return {
                get: get
            };
        }]);

}());
