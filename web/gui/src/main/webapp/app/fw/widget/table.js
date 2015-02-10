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

    var $window, fs,
        bottomMargin = 200;

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

    // Functions for creating a fixed header on a table (Angular Directive)

    function setTableWidth(t) {
        var tHeaders, tdElement, colWidth, numCols,
            winWidth = fs.windowSize().width;

        tHeaders = t.selectAll('th');
        numCols = tHeaders[0].length;
        colWidth = Math.floor(winWidth/numCols);

        tHeaders.each(function(thElement, index) {
            thElement = d3.select(this);

            tdElement = t.select('td:nth-of-type(' + (index + 1) + ')');

            // if the header has no text in it,
            // then make the column the width of the td element.
            // (This looks good for icons)
            if (!(thElement.html().length)) {
                var tdSize = tdElement.style('width');
                thElement.style('width', tdSize + 'px');
                tdElement.style('width', tdSize + 'px');
            }
            else {
                thElement.style('width', colWidth + 'px');
                tdElement.style('width', colWidth + 'px');
            }
        });
    }

    function setTableHeight(thead, tbody) {
        var winHeight = fs.windowSize().height;

        thead.style('display', 'block');
        tbody.style({'display': 'block',
            'height': ((winHeight - bottomMargin) + 'px'),
            'overflow': 'auto'
        });
    }

    function fixTable(t, th, tb, height) {
        setTableWidth(t);
        setTableHeight(th, tb, height);
    }

    angular.module('onosWidget')
        .factory('TableService', [function () {
            return {
                renderTable: renderTable
            };
        }])

        .directive('onosFixedHeader', ['$window', '$timeout',
            'MastService', 'FnService',
            function (_$window_, $timeout, mast, _fs_) {
            return function (scope, element) {
                $window = _$window_;
                fs = _fs_;
                var w = angular.element($window),
                    table = d3.select(element[0]),
                    shouldResize = false;

                scope.$watch(function () {
                    return {
                        h: window.innerHeight,
                        w: window.innerWidth
                    };
                }, function (newVal) {
                    var thead = table.select('thead'),
                        tbody = table.select('tbody');

                    scope.windowHeight = newVal.h;
                    scope.windowWidth = newVal.w;

                    scope.setTableHW = function () {
                        scope.$on('LastElement', function (event) {
                            fixTable(table, thead, tbody);
                            shouldResize = true;
                        });
                    };

                    if (shouldResize) {
                        fixTable(table, thead, tbody);
                    }

                }, true);

                w.bind('onos-fixed-header', function () {
                    scope.$apply();
                });
            };

        }]);

}());
