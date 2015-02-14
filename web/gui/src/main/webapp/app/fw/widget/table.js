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
        currCol = {},
        prevCol = {},
        tableIconTdSize = 33,
        bottomMargin = 200;

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

            if (tdElement.classed('table-icon')) {
                thElement.style('width', tableIconTdSize + 'px');
                tdElement.style('width', tableIconTdSize + 'px');
            } else {
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

    // Functions for sorting table rows by header and choosing appropriate icon

    function updateSortingIcons(thElem, api) {
        var div;
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

        div = thElem.select('div');
        api.sortNone(div);
        div = thElem.append('div');

        if (currCol.icon === 'tableColSortAsc') {
            api.sortAsc(div);
        } else {
            api.sortDesc(div);
        }

        if (prevCol.colId !== undefined &&
            prevCol.icon === 'tableColSortNone') {
            api.sortNone(prevCol.elem.select('div'));
        }

        prevCol.colId = currCol.colId;
        prevCol.elem = thElem;
    }

    function generateQueryParams() {
        var queryString = '?sortCol=' + currCol.colId + '&sortDir=';

        if(currCol.icon === 'tableColSortAsc') {
            queryString = queryString + 'asc';
        } else {
            queryString = queryString + 'desc';
        }

        return queryString;
    }

    angular.module('onosWidget')
        .directive('onosFixedHeader', ['$window', 'FnService',
            function (_$window_, _fs_) {
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
            return {
                scope: {
                    ctrlCallback: '&sortCallback'
                },
                link: function (scope, element) {
                    $log = _$log_;
                    is = _is_;
                    var table = d3.select(element[0]),
                        sortIconAPI = is.createSortIcon();

                    // when a header is clicked, change its icon tag
                    // and get sorting order to send to the server.
                    table.selectAll('th').on('click', function () {
                        var thElem = d3.select(this);

                        if (thElem.attr('sortable') === '') {
                            updateSortingIcons(thElem, sortIconAPI);
                            scope.ctrlCallback({
                                    urlSuffix: generateQueryParams()
                                });
                        }
                    });
                }
            };
        }]);

}());
