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

    function isF(f) {
        return $.isFunction(f) ? f : null;
    }

    // TODO: sensible defaults
    function createDragBehavior(force, selectCb, atDragEnd,
                                dragEnabled, clickEnabled) {
        var draggedThreshold = d3.scale.linear()
                .domain([0, 0.1])
                .range([5, 20])
                .clamp(true),
            drag,
            fSel = isF(selectCb),
            fEnd = isF(atDragEnd),
            fDEn = isF(dragEnabled),
            fCEn = isF(clickEnabled),
            bad = [];

        function naf(what) {
            return 'd3util.createDragBehavior(): '+ what + ' is not a function';
        }

        if (!fSel) {
            bad.push(naf('selectCb'));
        }
        if (!fEnd) {
            bad.push(naf('atDragEnd'));
        }
        if (!fDEn) {
            bad.push(naf('dragEnabled'));
        }
        if (!fCEn) {
            bad.push(naf('clickEnabled'));
        }

        if (bad.length) {
            alert(bad.join('\n'));
            return null;
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
                if (clickEnabled() || dragEnabled()) {
                    d3.event.sourceEvent.stopPropagation();

                    d.oldX = d.x;
                    d.oldY = d.y;
                    d.dragged = false;
                    d.fixed |= 2;
                    d.dragStarted = true;
                }
            })
            .on('drag', function(d) {
                if (dragEnabled()) {
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
                        // consider this the same as a 'click'
                        // (selection of a node)
                        if (clickEnabled()) {
                            selectCb(d, this); // TODO: set 'this' context instead of param
                        }
                    }
                    d.fixed &= ~6;

                    // hook at the end of a drag gesture
                    if (dragEnabled()) {
                        atDragEnd(d, this); // TODO: set 'this' context instead of param
                    }
                }
            });

        return drag;
    }

    function loadGlow(defs) {
        // TODO: parameterize color

        var glow = defs.append('filter')
            .attr('x', '-50%')
            .attr('y', '-50%')
            .attr('width', '200%')
            .attr('height', '200%')
            .attr('id', 'blue-glow');

        glow.append('feColorMatrix')
            .attr('type', 'matrix')
            .attr('values',
                '0 0 0 0  0 ' +
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

    // --- Ordinal scales for 7 values.
    // TODO: tune colors for light and dark themes
    // Note: These colors look good on the white background. Still, need to tune for dark.

    //               blue       brown      brick red  sea green  purple     dark teal  lime
    var lightNorm = ['#3E5780', '#78533B', '#CB4D28', '#018D61', '#8A2979', '#006D73', '#56AF00'],
        lightMute = ['#A8B8CC', '#CCB3A8', '#FFC2BD', '#96D6BF', '#D19FCE', '#8FCCCA', '#CAEAA4'],

        darkNorm  = ['#3E5780', '#78533B', '#CB4D28', '#018D61', '#8A2979', '#006D73', '#56AF00'],
        darkMute  = ['#A8B8CC', '#CCB3A8', '#FFC2BD', '#96D6BF', '#D19FCE', '#8FCCCA', '#CAEAA4'];

    function cat7() {
        var colors = {
                light: {
                    norm: d3.scale.ordinal().range(lightNorm),
                    mute: d3.scale.ordinal().range(lightMute)
                },
                dark: {
                    norm: d3.scale.ordinal().range(darkNorm),
                    mute: d3.scale.ordinal().range(darkMute)
                }
            },
            tcid = 'd3utilTestCard';

        function get(id, muted, theme) {
            // NOTE: since we are lazily assigning domain ids, we need to
            //       get the color from all 4 scales, to keep the domains
            //       in sync.
            var ln = colors.light.norm(id),
                lm = colors.light.mute(id),
                dn = colors.dark.norm(id),
                dm = colors.dark.mute(id);
            if (theme === 'dark') {
                return muted ? dm : dn;
            } else {
                return muted ? lm : ln;
            }
        }

        function testCard(svg) {
            var g = svg.select('g#' + tcid),
                dom = d3.range(7),
                k, muted, theme, what;

            if (!g.empty()) {
                g.remove();

            } else {
                g = svg.append('g')
                    .attr('id', tcid)
                    .attr('transform', 'scale(4)translate(20,20)');

                for (k=0; k<4; k++) {
                    muted = k%2;
                    what = muted ? ' muted' : ' normal';
                    theme = k < 2 ? 'light' : 'dark';
                    dom.forEach(function (id, i) {
                        var x = i * 20,
                            y = k * 20,
                            f = get(id, muted, theme);
                        g.append('circle').attr({
                            cx: x,
                            cy: y,
                            r: 5,
                            fill: f
                        });
                    });
                    g.append('rect').attr({
                        x: 140,
                        y: k * 20 - 5,
                        width: 32,
                        height: 10,
                        rx: 2,
                        fill: '#888'
                    });
                    g.append('text').text(theme + what)
                        .attr({
                            x: 142,
                            y: k * 20 + 2,
                            fill: 'white'
                        })
                        .style('font-size', '4pt');
                }
            }
        }

        return {
            testCard: testCard,
            get: get
        };
    }

    // === register the functions as a library
    onos.ui.addLib('d3util', {
        createDragBehavior: createDragBehavior,
        loadGlow: loadGlow,
        cat7: cat7
    });

}(ONOS));
