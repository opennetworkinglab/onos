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
 ONOS GUI -- Widget -- Table Service
 */
(function () {
    'use strict';

    var $log;

    // Render a plain d3 table by giving it the div, a config file, and data

    function renderTable(div, config, data) {
        var table = div.append('table'),
            colIds = config.colIds,
            colText = config.colText,
            dataObjects = data[Object.keys(data)[0]],
            thead, tbody, tRows;

        thead = table.append('thead');
        tbody = table.append('tbody');

        thead.append('tr').selectAll('th')
            .data(colText)
            .enter()
            .append('th')
            .text(function(d) { return d });

        tRows = tbody.selectAll('tr')
            .data(dataObjects)
            .enter()
            .append('tr');

        tRows.selectAll('td')
            .data(function(row) {
                return colIds.map(function(headerId) {
                    return {
                        column: headerId, value: row[headerId]
                    };
                });
            })
            .enter()
            .append('td')
            .html(function(d) { return d.value });

        return table;
    }

    // Functions for creating a fixed-header on a table (Angular Directive)

    function setColWidth(t) {
        var tHeaders, tdElement, colWidth;

        tHeaders = t.selectAll('th');

        // select each td in the first row and set the header's width to the
        // corresponding td's width, if td is larger than header's width.
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
            'height': height || '500px',
            'overflow': 'auto'
        });
    }

    function fixTable(t, th, tb, height) {
        setColWidth(t);
        setCSS(th, tb, height);
    }

    angular.module('onosWidget')
        .factory('TableService', ['$log', function (_$log_) {
            $log = _$log_;

            return {
                renderTable: renderTable
            };
        }])

        .directive('fixedHeader', ['$timeout', function ($timeout) {
                return {
                    restrict: 'A',
                    scope: {
                        tableHeight: '@'
                    },

                    link: function (scope, element) {
                        // TODO: look into other solutions than $timeout --
                        // fixed-header directive called before ng-repeat was
                        // finished; using $scope.$emit to notify this directive
                        // to fire was not working.
                        $timeout(function() {
                            var table = d3.select(element[0]),
                                thead = table.select('thead'),
                                tbody = table.select('tbody');

                            // wait until the table is visible
                            // (offsetParent returns null if display is "none")
                            scope.$watch(
                                function () {
                                    return (!(table.offsetParent === null));
                                },
                                function(newValue) {
                                    if (newValue === true) {

                                        // ensure thead and tbody have no display
                                        thead.style('display', null);
                                        tbody.style('display', null);

                                        $timeout(function () {
                                            fixTable(table, thead, tbody,
                                                scope.tableHeight);
                                        });
                                    }
                                });
                        }, 200);
                    }
                };
            }]);

}());
