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
 ONOS GUI -- Showing Icons Test Module
 */

(function () {
    'use strict';

    var config = {
        colIds: ['_iconid_available', 'id', 'mfr', 'hw', 'sw', 'serial',
            'annotations'],
        colText: ['Availability', 'URI', 'Vendor', 'Hardware Version',
            'Software Version', 'Serial Number', 'Protocol']
        },
        deviceData = {
            "devices": [{
                "id": "of:0000000000000001",
                "available": true,
                "_iconid_available": "deviceOnline",
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
                    "available": false,
                    "_iconid_available": "deviceOffline",
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
                    "id": "of:0000000000000092",
                    "available": false,
                    "_iconid_available": "deviceOffline",
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
                    "id": "of:0000000000000092",
                    "available": false,
                    "_iconid_available": "deviceOffline",
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
                    "id": "of:0000000000000092",
                    "available": false,
                    "_iconid_available": "deviceOffline",
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
                    "id": "of:0000000000000092",
                    "available": false,
                    "_iconid_available": "deviceOffline",
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
                    "id": "of:0000000000000092",
                    "available": false,
                    "_iconid_available": "deviceOffline",
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
                    "id": "of:0000000000000092",
                    "available": false,
                    "_iconid_available": "deviceOffline",
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
                    "id": "of:0000000000000092",
                    "available": false,
                    "_iconid_available": "deviceOffline",
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
                    "id": "of:0000000000000092",
                    "available": false,
                    "_iconid_available": "deviceOffline",
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
                    "id": "of:0000000000000092",
                    "available": false,
                    "_iconid_available": "deviceOffline",
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
                    "id": "of:0000000000000092",
                    "available": false,
                    "_iconid_available": "deviceOffline",
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
                    "id": "of:0000000000000092",
                    "available": false,
                    "_iconid_available": "deviceOffline",
                    "role": "MASTER",
                    "mfr": "Nicira, Inc.",
                    "hw": "Open vSwitch",
                    "sw": "2.0.1",
                    "serial": "None",
                    "annotations": {
                        "protocol": "OF_10"
                    }
                }]
        };

    function setColWidth(t) {
        var tHeaders, tdElement, colWidth;

        tHeaders = t.selectAll('th');

        // select each td in the first row and set the header's width to the
        // corresponding td's width, if td is larger than header's width
        tHeaders.each(function(thElement, index){
            thElement = d3.select(this);

            tdElement = t.select('td:nth-of-type(' + (index + 1) + ')');
            colWidth = tdElement.style('width');

            thElement.style('width', colWidth);
            tdElement.style('width', colWidth);
        });
    }

    function setCSS(thead, tbody, height) {
        thead.style('display', 'block');
        tbody.style({'display': 'block',
            'height': height || '100px',
            'overflow': 'auto'
        });
    }

    function fixTable(t, th, tb, height) {
        setColWidth(t);
        setCSS(th, tb, height);
    }

    angular.module('practiceTable', ['onosWidget'])

        .controller('showTableCtrl', ['$log', '$scope', '$rootScope',
            '$timeout', 'TableService',
            function ($log, $scope, $rootScope, $timeout, ts) {
                var table = ts.renderTable(d3.select('#tableDiv'), config, deviceData);

                // --- commented out code below were various attempts at $apply ---
                // will delete unneeded dependencies once we have figured out which ones we need

                //$timeout(function () {
                //        $log.log('inside timeout');
                //        //$scope.$watch('table', function (newVal, oldVal) {
                //            $scope.$apply();
                //        //});
                //    }, 1000);

                //$scope.$applyAsync();

                //$scope.$apply(function () {
                //    ts.renderTable(d3.select('#tableDiv'), config, deviceData);
                //});

                //$log.log($scope);

                //$scope.$watch('table', function(newVal, oldVal) {
                //    //if (newVal === oldVal) { $log.log('hello'); return; }
                //    $scope.$apply();
                //});
                //
                //$timeout(function () {
                //    $log.log("in timeout in controller");
                //    $rootScope.$watch(function () {
                //            return (table);
                //        },
                //        function(newValue, oldValue) {
                //            if(newValue) {
                //                $log.log('hello??');
                //                //$rootScope.$apply();
                //            }
                //        }
                //    );
                //    //$scope.$apply(table);
                //    $log.log("after scope.apply")});
            }])

        .directive('fixedHeader', ['$log', '$timeout', '$compile',
            function ($log, $timeout, $compile) {
             return {
                restrict: 'A',
                scope: {
                    tHeight: '@'
                },

                link: function (scope, element, attrs) {
                    $log.log("in directive");

                    var table = d3.select(element[0]),
                        thead = table.select('thead'),
                        tbody = table.select('tbody');

                    // wait until the table is visible
                    scope.$watch(
                        function () { return (!(table.offsetParent === null)); },
                        function(newValue, oldValue) {
                            if (newValue === true) {

                                // ensure thead and tbody have no display
                                thead.style('display', null);
                                tbody.style('display', null);

                                $timeout(function () {
                                    fixTable(table, thead, tbody, scope.tHeight);
                                });
                            }
                        });
                }
            };
        }]);
}());
