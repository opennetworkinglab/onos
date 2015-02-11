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

    var $log, $window, fs, is,
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

    function fixTable(t, th, tb) {
        setTableWidth(t);
        setTableHeight(th, tb);
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
                            // only adjust the table once it's completely loaded
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

        }])

        .directive('onosSortableHeader', ['$log', 'IconService',
            function (_$log_, _is_) {
            return function (scope, element, attrs) {
                $log = _$log_;
                is = _is_;
                var table = d3.select(element[0]),
                    currCol = {},
                    prevCol = {},
                    sortIconAPI = is.createSortIcon();

                // when a header is clicked, change its icon tag and get sorting
                // order to send to the server.
                table.selectAll('th').on('click', function () {
                    var thElem = d3.select(this),
                        div;

                    currCol.colId = thElem.attr('colId');

                    if (currCol.colId === prevCol.colId) {
                        (currCol.icon === 'tableColSortDesc') ?
                            currCol.icon = 'tableColSortAsc' :
                            currCol.icon = 'tableColSortDesc';
                        prevCol.icon = currCol.icon;
                    } else {
                        currCol.icon = 'tableColSortAsc';
                        prevCol.icon = 'tableColSortNone';
                    }

                    $log.debug('currCol clicked: ' + currCol.colId +
                    ', with sorting icon: ' + currCol.icon);
                    $log.debug('prevCol clicked: ' + prevCol.colId +
                    ', with its current sorting icon as ' + prevCol.icon);

                    div = thElem.select('div');
                    div.remove();

                    div = thElem.append('div');

                    if (currCol.icon === 'tableColSortAsc') {
                        sortIconAPI.sortAsc(div);
                    } else {
                        sortIconAPI.sortDesc(div);
                    }

                    if (prevCol.colId !== undefined &&
                        prevCol.icon === 'tableColSortNone') {
                        sortIconAPI.sortNone(prevCol.elem.select('div'));
                    }

                    prevCol.colId = currCol.colId;
                    prevCol.elem = thElem;

                });

                // TODO: send the prev and currCol info to the server to use in sorting table

                // TODO: figure out timing of events:
                // updating the icon
                // sending the column sorting info to the server
                // refreshing the table so that the new rows will be sorted

            }
        }]);

}());
