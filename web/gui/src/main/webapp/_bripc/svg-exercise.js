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
 ONOS GUI -- SVG mouse over d3 exercise module
 */

(function () {
    'use strict';

    // injected references
    var $log, fs;

    // constants
    var btnWidth = 175,
        btnHeight = 50,
        hoverZone = 60,
        sectorDivisions = 3,
        pageMargin = 20;

    // svg elements
    var svg, g;

    // other variables
    var winWidth, winHeight,
        sectorWidth, sectorHeight,
        currSector = 4,
        mouse;

    // ====================================================

    // helper functions
    function centeredOnWindow(axis, dim) {
        return (axis / 2) - (dim / 2);
    }

    // ====================================================

    function center(elem) {
        $log.debug(winWidth / 2);
        $log.debug(winHeight / 2);
        $log.debug((winWidth / 2) - ((elem.node().getBBox().width) / 2));
        $log.debug((winHeight / 2) - ((elem.node().getBBox().height) / 2));
        return {
            x: (winWidth / 2) - ((elem.node().getBBox().width) / 2),
            y: (winHeight / 2) - ((elem.node().getBBox().height) / 2)
        }
    }

    function showSectors() {
        svg.append('line')
            .attr({
                x1: winWidth / 2,
                x2: winWidth / 2,
                y1: 0,
                y2: winHeight,
                stroke: 'purple',
                'stroke-width': '3px'
            });
        svg.append('line')
            .attr({
                x1: 0,
                x2: winWidth,
                y1: winHeight / 2,
                y2: winHeight / 2,
                stroke: 'purple',
                'stroke-width': '3px'
            });
        for (var i = 1; i < 5; i++) {
            var j;
            if (i < 3) {
                j = i;
                svg.append('line')
                    .attr({
                        x1: sectorWidth * j,
                        x2: sectorWidth * j,
                        y1: 0,
                        y2: winHeight,
                        stroke: 'red',
                        'stroke-width': '3px'
                    });
            }
            else {
                j = i - 2;
                svg.append('line')
                    .attr({
                        x1: 0,
                        x2: winWidth,
                        y1: sectorHeight * j,
                        y2: sectorHeight * j,
                        stroke: 'red',
                        'stroke-width': '3px'
                    });
            }
        }
    }

    function onMouseMove() {
        mouse = d3.mouse(this);
        moveButton();
    }

    function removeMouseListener() {
        g.on('mousemove', null);
    }

    function addMouseListener() {
        g.on('mousemove', onMouseMove);
    }

    function selectSector() {
        var sector, row, col;

        do {
            sector = Math.floor((Math.random() * 9));
        } while (sector === currSector);
        $log.debug('sector after loop: ' + sector);
        $log.debug('currSector after loop: ' + currSector);
        currSector = sector;
        $log.debug('currSector after assignment: ' + currSector);
        row = Math.floor(sector / sectorDivisions);
        col = sector % sectorDivisions;

        $log.debug('row: ' + row);
        $log.debug('col: ' + col);

        return {
            xmin: sectorWidth * col,
            xmax: (sectorWidth * col) + sectorWidth,
            ymin: sectorHeight * row,
            ymax: (sectorHeight * row) + sectorHeight
        }
    }

    function selectXY(sectorCoords) {
        var x, y, x1, y1;
        do {
            x = (Math.random() * sectorCoords.xmax) + sectorCoords.xmin;
            y = (Math.random() * sectorCoords.ymax) + sectorCoords.ymin;
            x1 = x + btnWidth;
            y1 = y + btnHeight;
        } while (x1 >= winWidth - pageMargin || y1 >= winHeight - pageMargin);

        return {
            x: x,
            y: y
        }
    }

    function gTranslate(x, y) {
        return 'translate(' + x + ',' + y + ')';
    }

    function moveButton() {
        var sec = selectSector(),
            pos = selectXY(sec);
        $log.debug(pos.x, pos.y);
        g.transition()
            .duration(400)
            .ease('cubic-out')
            .each('start', removeMouseListener)
            .attr('transform', gTranslate(pos.x, pos.y))
            .each('end', addMouseListener);
    }

    angular.module('svgExercise', ['onosUtil'])

        .controller('svgExCtrl', ['$log', function (_$log_) {
            $log = _$log_;
        }])

        .directive('improvePerformance', ['FnService', function (_fs_) {
            fs = _fs_;
            return {
                restrict: 'E',
                link: function (scope, elem, attrs) {
                    winWidth = fs.windowSize().width;
                    winHeight = fs.windowSize().height;
                    // getting rid of pageMargin to see if the math is easier
                    // could put the padding somewhere else as in where it's ok to move the button
                    //sectorWidth = (winWidth / sectorDivisions) - pageMargin;
                    //sectorHeight = (winHeight / sectorDivisions) - pageMargin;
                    sectorWidth = winWidth / sectorDivisions;
                    sectorHeight = winHeight / sectorDivisions;

                    svg = d3.select(elem[0])
                        .append('svg')
                        .attr({
                            width: winWidth + 'px',
                            height: winHeight + 'px'
                        });

                    showSectors();
                    g = svg.append('g');

                    var button = g.append('rect')
                            .attr({
                                width: btnWidth + 'px',
                                height: btnHeight + 'px',
                                rx: '10px',
                                'class': 'button'
                            }),
                        text = g.append('text')
                            .style('text-anchor', 'middle')
                            .text('Click for better performance.')
                            .attr({
                                x: btnWidth / 2,
                                y: (btnHeight / 2) + 5
                            }),
                        rect = g.append('rect')
                            .attr({
                                fill: 'none',
                                'pointer-events': 'all',
                                width: btnWidth + hoverZone + 'px',
                                height: btnHeight + hoverZone + 'px',
                                x: -(hoverZone / 2),
                                y: -(hoverZone / 2)
                            }),
                        centeredG = center(g);
                    g.attr('transform',
                        gTranslate(centeredG.x, centeredG.y));
                    //gTranslate(centeredOnWindow(winWidth, btnWidth),
                    //           centeredOnWindow(winHeight, btnHeight)));

                    addMouseListener();
                }
            };
        }]);
}());