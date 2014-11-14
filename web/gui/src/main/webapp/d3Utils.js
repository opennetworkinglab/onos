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
 Utility functions for D3 visualizations.

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    function createDragBehavior(force, selectCb, atDragEnd, requireMeta) {
        var draggedThreshold = d3.scale.linear()
                .domain([0, 0.1])
                .range([5, 20])
                .clamp(true),
            drag;

        // TODO: better validation of parameters
        if (!$.isFunction(selectCb)) {
            alert('d3util.createDragBehavior(): selectCb is not a function')
        }
        if (!$.isFunction(atDragEnd)) {
            alert('d3util.createDragBehavior(): atDragEnd is not a function')
        }
        if (!$.isFunction(requireMeta)) {
            alert('d3util.createDragBehavior(): requireMeta is not a function')
        }

        function dragged(d) {
            var threshold = draggedThreshold(force.alpha()),
                dx = d.oldX - d.px,
                dy = d.oldY - d.py;
            if (Math.abs(dx) >= threshold || Math.abs(dy) >= threshold) {
                d.dragged = true;
            }
            return d.dragged;
        }

        drag = d3.behavior.drag()
            .origin(function(d) { return d; })
            .on('dragstart', function(d) {
                if (requireMeta() ^ !d3.event.sourceEvent.metaKey) {
                    d3.event.sourceEvent.stopPropagation();

                    d.oldX = d.x;
                    d.oldY = d.y;
                    d.dragged = false;
                    d.fixed |= 2;
                    d.dragStarted = true;
                }
            })
            .on('drag', function(d) {
                if (requireMeta() ^ !d3.event.sourceEvent.metaKey) {
                    d.px = d3.event.x;
                    d.py = d3.event.y;
                    if (dragged(d)) {
                        if (!force.alpha()) {
                            force.alpha(.025);
                        }
                    }
                }
            })
            .on('dragend', function(d) {
                if (d.dragStarted) {
                    d.dragStarted = false;
                    if (!dragged(d)) {
                        // consider this the same as a 'click' (selection of node)
                        selectCb(d, this); // TODO: set 'this' context instead of param
                    }
                    d.fixed &= ~6;

                    // hook at the end of a drag gesture
                    atDragEnd(d, this); // TODO: set 'this' context instead of param
                }
            });

        return drag;
    }

    function appendGlow(svg) {
        // TODO: parameterize color

        var glow = svg.append('filter')
            .attr('x', '-50%')
            .attr('y', '-50%')
            .attr('width', '200%')
            .attr('height', '200%')
            .attr('id', 'blue-glow');

        glow.append('feColorMatrix')
            .attr('type', 'matrix')
            .attr('values', '0 0 0 0  0 ' +
            '0 0 0 0  0 ' +
            '0 0 0 0  .7 ' +
            '0 0 0 1  0 ');

        glow.append('feGaussianBlur')
            .attr('stdDeviation', 3)
            .attr('result', 'coloredBlur');

        glow.append('feMerge').selectAll('feMergeNode')
            .data(['coloredBlur', 'SourceGraphic'])
            .enter().append('feMergeNode')
            .attr('in', String);
    }

    // === register the functions as a library
    onos.ui.addLib('d3util', {
        createDragBehavior: createDragBehavior,
        appendGlow: appendGlow
    });

}(ONOS));
