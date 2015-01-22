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

    function setColWidth($log, t) {
        var tHeaders, tdElement;

        // for each th, set its width to td's width
        // do this by looping through each td in the first row
        // and finding its column width
        tHeaders = t.selectAll('th');
        //loop through selectAll array
        tHeaders.forEach(function(thElement){
            tdElement = t.filter('nth-child()');
            $log.log(thElement);
        });
    }

    function setCSS(thead, tbody) {

    }

    function trimTable(tbody) {

    }

    function fixTable($log, t, th, tb) {
        setColWidth($log, t);
        setCSS(th, tb);
        trimTable(tb);
    }

    angular.module('practiceTable', [])

        .directive('fixedHeader', ['$log', '$timeout', function ($log, $timeout) {
            return {
                restrict: 'A',

                link: function (scope, element, attrs) {
                    var table = d3.select(element[0]),
                        thead = table.select('thead'),
                        tbody = table.select('tbody');

                    // wait until the table is visible
                    scope.$watch(
                        function () { return (!(table.offsetParent === null)); },
                        function (newValue, oldValue) {
                            if (newValue === true) {
                                $log.log('table is visible');

                                // ensure thead and tbody have no display
                                thead.style('display', null);
                                tbody.style('display', null);

                                $timeout(function() {
                                    $log.log('timeout is over');
                                    fixTable($log, table, thead, tbody);
                                }, 200);
                            }
                        });
                    $log.log('fixedHeader directive has been created');
                }
            };
        }]);
}());

$scope.$watch(function () { return $elem.find("tbody").is(':visible') },
    function (newValue, oldValue) {
        if (newValue === true) {
            // reset display styles so column widths are correct when measured below
            $elem.find('thead, tbody, tfoot').css('display', '');

            // wrap in $timeout to give table a chance to finish rendering
            $timeout(function () {
                // set widths of columns
                $elem.find('th').each(function (i, thElem) {
                    thElem = $(thElem);
                    var tdElems = $elem.find('tbody tr:first td:nth-child(' + (i + 1) + ')');
                    var tfElems = $elem.find('tfoot tr:first td:nth-child(' + (i + 1) + ')');

                    var columnWidth = tdElems.width();
                    thElem.width(columnWidth);
                    tdElems.width(columnWidth);
                    tfElems.width(columnWidth);
                });

                // set css styles on thead and tbody
                $elem.find('thead, tfoot').css({
                    'display': 'block',
                });

                $elem.find('tbody').css({
                    'display': 'block',
                    'height': $scope.tableHeight || '400px',
                    'overflow': 'auto'
                });

                // reduce width of last column by width of scrollbar
                var scrollBarWidth = $elem.find('thead').width() - $elem.find('tbody')[0].clientWidth;
                if (scrollBarWidth > 0) {
                    // for some reason trimming the width by 2px lines everything up better
                    scrollBarWidth -= 2;
                    $elem.find('tbody tr:first td:last-child').each(function (i, elem) {
                        $(elem).width($(elem).width() - scrollBarWidth);
                    });
                }
            });
        }
    });
}