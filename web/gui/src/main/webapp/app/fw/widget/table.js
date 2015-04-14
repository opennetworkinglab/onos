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
        pdg = 12,
        colWidth = 'col-width',
        tableIcon = 'table-icon';

    // internal state
    var currCol = {},
        prevCol = {};

    // Functions for creating a fixed header on a table (Angular Directive)

    function setElemWidth(elem, size) {
        elem.style('width', size + 'px')
    }

    function setColWidth(th, td, size) {
        setElemWidth(th, size);
        setElemWidth(td, size);
    }

    // count number of headers of
    //   - assigned width,
    //   - icon width,
    //   - and default width
    // assumes assigned width is not given to icons
    // returns the width of all columns that are not icons
    // or have an assigned width
    function getDefaultWidth(headers) {
        var winWidth = fs.windowSize().width,
            iconCols = 0,
            regCols = 0,
            cstmColWidth = 0;

        headers.each(function (d, i) {
            var thElement = d3.select(this),
                cstmWidth = thElement.attr(colWidth);

            if (cstmWidth) {
                cstmColWidth += fs.noPx(cstmWidth);
            } else if (thElement.classed(tableIcon)) {
                iconCols += 1;
            } else {
                regCols += 1;
            }
        });

        return Math.floor((winWidth - cstmColWidth -
                            (iconCols * tableIconTdSize)) / regCols);
    }

    function setTableWidth(t) {
        var tHeaders = t.selectAll('th'),
            defaultColWidth = getDefaultWidth(tHeaders);

        tHeaders.each(function (d, i) {
            var thElement = d3.select(this),
                tr = t.select('tr:nth-of-type(2)'),
                tdElement = tr.select('td:nth-of-type(' + (i + 1) + ')'),
                custWidth = thElement.attr(colWidth);

            if (custWidth) {
                setColWidth(thElement, tdElement, fs.noPx(custWidth));
            } else if (thElement.classed(tableIcon)) {
                setColWidth(thElement, tdElement, tableIconTdSize);
            } else {
                setColWidth(thElement, tdElement, defaultColWidth);
            }
        });
    }

    // get the size of the window and then subtract the extra space at the top
    // to get the height of the table
    function setTableHeight(thead, tbody) {
        var ttlHgt = fs.noPxStyle(d3.select('.tabular-header'), 'height'),
            thHgt = fs.noPxStyle(thead, 'height'),
            totalHgt = ttlHgt + thHgt + pdg,
            tbleHgt = fs.windowSize(mast.mastHeight() + totalHgt).height;

        thead.style('display', 'block');
        tbody.style({
            display: 'block',
            height: tbleHgt + 'px',
            overflow: 'auto'
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
            (currCol.icon === 'downArrow') ?
                currCol.icon = 'upArrow' :
                currCol.icon = 'downArrow';
            prevCol.icon = currCol.icon;
        } else {
            currCol.icon = 'upArrow';
            prevCol.icon = 'tableColSortNone';
        }

        div = thElem.select('div');
        api.sortNone(div);
        div = thElem.append('div');

        if (currCol.icon === 'upArrow') {
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

    function sortRequestParams() {
        return {
            sortCol: currCol.colId,
            sortDir: (currCol.icon === 'upArrow' ? 'asc' : 'desc')
        };
    }

    angular.module('onosWidget')
        .directive('onosFixedHeader', ['$window', 'FnService', 'MastService',
            function (_$window_, _fs_, _mast_) {
            return function (scope, element) {
                $window = _$window_;
                fs = _fs_;
                mast = _mast_;

                var w = angular.element($window),
                    table = d3.select(element[0]),
                    thead = table.select('thead'),
                    tbody = table.select('tbody'),
                    canAdjust = false;

                scope.$watch(function () {
                    return {
                        h: window.innerHeight,
                        w: window.innerWidth
                    };
                }, function (newVal) {
                    var wsz = fs.windowSize(0, 30);
                    scope.windowHeight = newVal.h;
                    scope.windowWidth = newVal.w;

                    // default table size in case no data elements
                    table.style('width', wsz.width + 'px');

                    scope.$on('LastElement', function () {
                        // only adjust the table once it's completely loaded
                        fixTable(table, thead, tbody);
                        canAdjust = true;
                    });

                    if (canAdjust) {
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
                                requestParams: sortRequestParams()
                            });
                        }
                    });
                }
            };
        }]);

}());
