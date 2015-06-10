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

    // injected refs
    var $log, $window, fs, mast, is;

    // constants
    var tableIconTdSize = 33,
        pdg = 22,
        colWidth = 'col-width',
        tableIcon = 'table-icon',
        asc = 'asc',
        desc = 'desc',
        none = 'none';

    // internal state
    var currCol = {},
        prevCol = {},
        sortIconAPI;

    // Functions for creating a scrolling table body with fixed table header

    function _width(elem, width) {
        elem.style('width', width);
    }

    function defaultSize(table, width) {
        var thead = table.select('.table-header').select('table'),
            tbody = table.select('.table-body').select('table'),
            wpx = width + 'px';
        _width(thead, wpx);
        _width(tbody, wpx);
    }

    function adjustTable(table, width, height) {
        var thead = table.select('.table-header').select('table'),
            tbodyDiv = table.select('.table-body'),
            tbody = tbodyDiv.select('table'),
            cstmWidths = {};

        function findCstmWidths() {
            var headers = thead.selectAll('td');

            headers.each(function (d, i) {
                var h = d3.select(this),
                    index = i.toString();
                if (h.classed(tableIcon)) {
                    cstmWidths[index] = tableIconTdSize + 'px';
                }
                if (h.attr(colWidth)) {
                    cstmWidths[index] = h.attr(colWidth);
                }
            });
            if (fs.debugOn('widget')) {
                $log.debug('Headers with custom widths: ', cstmWidths);
            }
        }

        function setTdWidths(elem) {
            var tds = elem.selectAll('tr:not(.ignore-width)').selectAll('td');
            _width(elem, width + 'px');

            tds.each(function (d, i) {
                var td = d3.select(this),
                    index = i.toString();
                if (cstmWidths.hasOwnProperty(index)) {
                    _width(td, cstmWidths[index]);
                }
            });
        }

        function setHeight(body) {
            var h = height - (mast.mastHeight() +
                fs.noPxStyle(d3.select('.tabular-header'), 'height') +
                fs.noPxStyle(thead, 'height') + pdg);
            body.style('height', h + 'px');
        }

        findCstmWidths();
        setTdWidths(thead);
        setTdWidths(tbody);
        setHeight(tbodyDiv);

        cstmWidths = {};
    }

    // Functions for sorting table rows by header

    function updateSortDirection(thElem) {
        sortIconAPI.sortNone(thElem.select('div'));
        currCol.div = thElem.append('div');
        currCol.colId = thElem.attr('colId');

        if (currCol.colId === prevCol.colId) {
            (currCol.dir === desc) ? currCol.dir = asc : currCol.dir = desc;
            prevCol.dir = currCol.dir;
        } else {
            currCol.dir = asc;
            prevCol.dir = none;
        }
        (currCol.dir === asc) ?
            sortIconAPI.sortAsc(currCol.div) : sortIconAPI.sortDesc(currCol.div);

        if (prevCol.colId && prevCol.dir === none) {
            sortIconAPI.sortNone(prevCol.div);
        }

        prevCol.colId = currCol.colId;
        prevCol.div = currCol.div;
    }

    function sortRequestParams() {
        return {
            sortCol: currCol.colId,
            sortDir: currCol.dir
        };
    }

    function resetSort() {
        if (currCol.div) {
            sortIconAPI.sortNone(currCol.div);
        }
        if (prevCol.div) {
            sortIconAPI.sortNone(prevCol.div);
        }
        currCol = {};
        prevCol = {};
    }

    angular.module('onosWidget')
        .directive('onosFixedHeader', ['$log','$window',
            'FnService', 'MastService',

            function (_$log_, _$window_, _fs_, _mast_) {
            return function (scope, element) {
                $log = _$log_;
                $window = _$window_;
                fs = _fs_;
                mast = _mast_;

                var table = d3.select(element[0]),
                    canAdjust = false;

                scope.$watchCollection(function () {
                    return {
                        h: $window.innerHeight,
                        w: $window.innerWidth
                    };
                }, function () {
                    var wsz = fs.windowSize(0, 30),
                        wWidth = wsz.width,
                        wHeight = wsz.height;

                    if (!scope.tableData.length) {
                        defaultSize(table, wWidth);
                    }

                    scope.$on('LastElement', function () {
                        // only adjust the table once it's completely loaded
                        adjustTable(table, wWidth, wHeight);
                        canAdjust = true;
                    });

                    if (canAdjust) {
                        adjustTable(table, wWidth, wHeight);
                    }
                });
            };
        }])

        .directive('onosSortableHeader', ['$log', 'IconService',
            function (_$log_, _is_) {
            return {
                scope: {
                    sortCallback: '&',
                    sortParams: '='
                },
                link: function (scope, element) {
                    $log = _$log_;
                    is = _is_;
                    var header = d3.select(element[0]);
                        sortIconAPI = is.sortIcons();

                    // when a header is clicked, change its sort direction
                    // and get sorting order to send to the server.
                    header.selectAll('td').on('click', function () {
                        var col = d3.select(this);

                        if (col.attr('sortable') === '') {
                            updateSortDirection(col);
                            scope.$apply(function () {
                                scope.sortParams = sortRequestParams();
                            });
                            scope.sortCallback({
                                requestParams: scope.sortParams
                            });
                        }
                    });
                }
            };
        }])

        .factory('TableService', ['IconService',

            function (is) {
                sortIconAPI = is.sortIcons();

                return {
                    resetSort: resetSort
                };
        }]);

}());
