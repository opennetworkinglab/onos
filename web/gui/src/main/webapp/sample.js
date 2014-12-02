/*
 * Copyright 2014 Open Networking Laboratory
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
 Sample module file to illustrate framework integration.

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    var pi = Math.PI,
        svg,
        dotG,
        nCircles = 12,
        circleData = [],
        dotId = 0,
        angle = 360 / nCircles,
        baseAngle = -90 - angle,
        groupRadius = 120,
        dotRadius = 24,
        dotMoveMs = 800,
        dotAppearMs = 300,
        dotEase = 'elastic',
        colorScale = d3.scale.linear()
            .domain([-pi/2, 2*pi/4, 3*pi/2])
            .range(['green', 'goldenrod', 'blue']);

    // set the size of the SVG layer to match that of the view
    function sizeSvg(view) {
        svg.attr({
            width: view.width(),
            height: view.height()
        });
    }

    // gets invoked only the first time the view is loaded
    function init(view, ctx, flags) {
        // prepare our SVG layer...
        svg = view.$div.append('svg');
        sizeSvg(view);
        dotG = svg.append('g').attr('id', 'dots');
    }

    // gets invoked just before our view is loaded
    function reset(view, ctx, flags) {
        // clear dot group and reset circle data
        dotG.html('');
        circleData = [];
        // also clear text, if any
        svg.selectAll('text').remove();
    }

    function updateCirclePositions(view, addNew) {
        var w = view.width(),
            h = view.height(),
            ox = w / 2,
            oy = h / 2;

        // reposition existing dots
        circleData.forEach(function (c, i) {
            var inc = addNew ? 1 : 0,
                theta = ((i + inc) * angle + baseAngle) * pi/180,
                dx = Math.cos(theta) * groupRadius,
                dy = Math.sin(theta) * groupRadius,
                x = ox + dx,
                y = oy + dy;
            if (!addNew && i === 0) {
                x = ox;
                y = oy;
            }
            c.cx = x;
            c.cy = y;
            c.rgb = colorScale(theta);
        });

        if (addNew) {
            // introduce a new dot
            circleData.unshift({
                cx: ox,
                cy: oy,
                id: dotId++
            });
        }

        // +1 to account for the circle in the center..
        if (circleData.length > nCircles + 1) {
            circleData.splice(nCircles + 1, 1);
        }
    }

    function doCircles(view) {
        var ox = view.width() / 2,
            oy = view.height() / 2,
            stroke = 'black',
            fill = 'red',
            hoverFill = 'magenta';

        // move existing circles, and add a new one
        updateCirclePositions(view, true);

        var circ = dotG.selectAll('circle')
            .data(circleData, function (d) { return d.id; });

        // operate on existing elements
        circ.on('mouseover', null)
            .on('mouseout', null)
            .on('click', null)
            .transition()
            .duration(dotMoveMs)
            .ease(dotEase)
            .attr({
                cx: function (d) { return d.cx; },
                cy: function (d) { return d.cy; }
            })
            .style({
                cursor: 'default',
                fill: function (d) { return d.rgb; }
            });

        // operate on entering elements
        circ.enter()
            .append('circle')
            .attr({
                cx: function (d) { return d.cx; },
                cy: function (d) { return d.cy; },
                r: 0
            })
            .style({
                fill: fill,
                stroke: stroke,
                'stroke-width': 3.5,
                cursor: 'pointer',
                opacity: 0
            })
            .on('mouseover', function (d) {
                d3.select(this).style('fill', hoverFill);
            })
            .on('mouseout', function (d) {
                d3.select(this).style('fill', fill);
            })
            .on('click', function (d) {
                setTimeout(function() {
                    doCircles(view, true);
                }, 10);
            })
            .transition()
            .delay(dotMoveMs)
            .duration(dotAppearMs)
            .attr('r', dotRadius)
            .style('opacity', 1);

        // operate on exiting elements
        circ.exit()
            .transition()
            .duration(750)
            .style('opacity', 0)
            .attr({
                cx: ox,
                cy: oy,
                r: groupRadius - dotRadius
            })
            .remove();
    }

    function load(view, ctx, flags) {
        var ctxText = ctx ? 'Context is "' + ctx + '"' : '';

        // display our view context
        if (ctxText) {
            svg.append('text')
                .text(ctxText)
                .attr({
                    x: 20,
                    y: '1.5em'
                })
                .style({
                    fill: 'darkgreen',
                    'font-size': '20pt'
                });
        }

        doCircles(view);
    }

    function resize(view, ctx, flags) {
        sizeSvg(view);
        updateCirclePositions(view);

        // move exiting dots into new positions, relative to view size
        var circ = dotG.selectAll('circle')
            .data(circleData, function (d) { return d.id; });
        circ.attr({
                cx: function (d) { return d.cx; },
                cy: function (d) { return d.cy; }
            });
    }

    // == register our view here, with links to lifecycle callbacks

    onos.ui.addView('sample', {
        init: init,
        reset: reset,
        load: load,
        resize: resize
    });

}(ONOS));
