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
        flashTime = 1500,
        colWidth = 'col-width',
        tableIcon = 'table-icon',
        asc = 'asc',
        desc = 'desc',
        none = 'none';

    // internal state
    var currCol = {},
        prevCol = {},
        cstmWidths = {},
        sortIconAPI;

    // Functions for resizing a tabular view to the window

    function _width(elem, width) {
        elem.style('width', width);
    }

    function findCstmWidths(table) {
        var headers = table.select('.table-header').selectAll('td');

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

    function setTdWidths(elem, width) {
        var tds = elem.select('tr:first-child').selectAll('td');
        _width(elem, width + 'px');

        tds.each(function (d, i) {
            var td = d3.select(this),
                index = i.toString();
            if (cstmWidths.hasOwnProperty(index)) {
                _width(td, cstmWidths[index]);
            }
        });
    }

    function setHeight(thead, body, height) {
        var h = height - (mast.mastHeight() +
            fs.noPxStyle(d3.select('.tabular-header'), 'height') +
            fs.noPxStyle(thead, 'height') + pdg);
        body.style('height', h + 'px');
    }

    function adjustTable(haveItems, tableElems, width, height) {
        if (haveItems) {
            setTdWidths(tableElems.thead, width);
            setTdWidths(tableElems.tbody, width);
            setHeight(tableElems.thead, tableElems.table.select('.table-body'), height);
        } else {
            setTdWidths(tableElems.thead, width);
            _width(tableElems.tbody, width + 'px');
        }
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
        .directive('onosTableResize', ['$log','$window',
            'FnService', 'MastService',

            function (_$log_, _$window_, _fs_, _mast_) {
            return function (scope, element) {
                $log = _$log_;
                $window = _$window_;
                fs = _fs_;
                mast = _mast_;

                var table = d3.select(element[0]),
                    tableElems = {
                        table: table,
                        thead: table.select('.table-header').select('table'),
                        tbody: table.select('.table-body').select('table')
                    },
                    wsz;

                findCstmWidths(table);

                // adjust table on window resize
                scope.$watchCollection(function () {
                    return {
                        h: $window.innerHeight,
                        w: $window.innerWidth
                    };
                }, function () {
                    wsz = fs.windowSize(0, 30);
                    adjustTable(
                        scope.tableData.length,
                        tableElems,
                        wsz.width, wsz.height
                    );
                });

                // adjust table when data changes
                scope.$watchCollection('tableData', function () {
                    adjustTable(
                        scope.tableData.length,
                        tableElems,
                        wsz.width, wsz.height
                    );
                });

                scope.$on('$destroy', function () {
                    cstmWidths = {};
                });
            };
        }])

        .directive('onosSortableHeader', ['$log', 'IconService',
            function (_$log_, _is_) {
            return function (scope, element) {
                $log = _$log_;
                is = _is_;
                var header = d3.select(element[0]);
                    sortIconAPI = is.sortIcons();

                header.selectAll('td').on('click', function () {
                    var col = d3.select(this);

                    if (col.attr('sortable') === '') {
                        updateSortDirection(col);
                        scope.sortParams = sortRequestParams();
                        scope.sortCallback(scope.sortParams);
                    }
                });

                scope.$on('$destroy', function () {
                    resetSort();
                });
            };
        }])

        .directive('onosFlashChanges',
            ['$log', '$parse', '$timeout', 'FnService',
            function ($log, $parse, $timeout, fs) {

            return function (scope, element, attrs) {
                var idProp = attrs.idProp,
                    table = d3.select(element[0]),
                    trs, promise;

                function highlightRows() {
                    var changedRows = [];
                    function classRows(b) {
                        if (changedRows.length) {
                            angular.forEach(changedRows, function (tr) {
                                tr.classed('data-change', b);
                            });
                        }
                    }
                    // timeout because 'row-id' was the un-interpolated value
                    // "{{link.one}}" for example, instead of link.one evaluated
                    // timeout executes on the next digest -- after evaluation
                    $timeout(function () {
                        if (scope.tableData.length) {
                            trs = table.selectAll('tr');
                        }

                        if (trs && !trs.empty()) {
                            trs.each(function () {
                                var tr = d3.select(this);
                                if (fs.find(tr.attr('row-id'),
                                        scope.changedData,
                                        idProp) > -1) {
                                    changedRows.push(tr);
                                }
                            });
                            classRows(true);
                            promise = $timeout(function () {
                                classRows(false);
                            }, flashTime);
                            trs = undefined;
                        }
                    });
                }

                // new items added:
                scope.$on('ngRepeatComplete', highlightRows);
                // items changed in existing set:
                scope.$watchCollection('changedData', highlightRows);

                scope.$on('$destroy', function () {
                    if (promise) {
                        $timeout.cancel(promise);
                    }
                });
            };
        }]);

}());
