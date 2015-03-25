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
        pageMargin = 10;

    // svg elements
    var svg, g;

    // other variables
    var winWidth, winHeight,
        sectorWidth, sectorHeight,
        currSector = 4, // always starts in the middle
        mouse;

    // ====================================================

    function centeredOnWindow(axis, dim) {
        return (axis / 2) - (dim / 2);
    }

    function showSectors() {
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
        var sector, row, col,
            currSectorCol = currSector % sectorDivisions;

        do {
            sector = Math.floor((Math.random() * 9));
            col = sector % sectorDivisions;
        } while (col === currSectorCol);
        currSector = sector;
        row = Math.floor(sector / sectorDivisions);

        // active area is the coordinates of the sector, plus or minus a margin
        return {
            xmin: (sectorWidth * col) + pageMargin,
            xmax: ((sectorWidth * col) + sectorWidth) - pageMargin,
            ymin: (sectorHeight * row) + pageMargin,
            ymax: ((sectorHeight * row) + sectorHeight) - pageMargin
        }
    }

    function selectXY(sectorCoords) {
        var x, y, x1, y1;

        do {
            x = (Math.random() * sectorCoords.xmax) + sectorCoords.xmin;
            y = (Math.random() * sectorCoords.ymax) + sectorCoords.ymin;
            x1 = x + btnWidth;
            y1 = y + btnHeight;
        } while (x1 > sectorCoords.xmax || y1 > sectorCoords.ymax);

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
        g.transition()
            .duration(300)
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
                link: function (scope, elem) {
                    winWidth = fs.windowSize().width;
                    winHeight = fs.windowSize().height;
                    sectorWidth = winWidth / sectorDivisions;
                    sectorHeight = winHeight / sectorDivisions;

                    svg = d3.select(elem[0])
                        .append('svg')
                        .attr({
                            width: winWidth + 'px',
                            height: winHeight + 'px'
                        });

                    //showSectors();
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
                            });
                    g.attr('transform',
                        gTranslate(centeredOnWindow(winWidth, btnWidth),
                                   centeredOnWindow(winHeight, btnHeight)));

                    addMouseListener();
                }
            };
        }]);
}());