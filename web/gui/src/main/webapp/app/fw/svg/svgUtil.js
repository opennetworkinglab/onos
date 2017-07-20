/*
 * Copyright 2015-present Open Networking Laboratory
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
 ONOS GUI -- SVG -- Util Service
 */

/*
 The SVG Util Service provides a miscellany of utility functions.
 */

(function () {
    'use strict';

    // injected references
    var $log, fs;

    // TODO: change 'force' ref to be 'force.alpha' ref.
    function createDragBehavior(force, selectCb, atDragEnd,
                                dragEnabled, clickEnabled, zs) {
        var draggedThreshold = d3.scale.linear()
            .domain([0, 0.1])
            .range([5, 20])
            .clamp(true),
            drag,
            fSel = fs.isF(selectCb),
            fEnd = fs.isF(atDragEnd),
            fDEn = fs.isF(dragEnabled),
            fCEn = fs.isF(clickEnabled),
            bad = [];

        function naf(what) {
            return 'SvgUtilService: createDragBehavior(): ' + what +
                ' is not a function';
        }

        if (!force) {
            bad.push('SvgUtilService: createDragBehavior(): ' +
                'Bad force reference');
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
            $log.error(bad.join('\n'));
            return null;
        }

        function dragged(d) {
            var scale = zs ? zs.scale() : 1,
                threshold = draggedThreshold(force.alpha()) / scale,
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
                d3.event.sourceEvent.stopPropagation();

                if (d.dragStarted) {
                    d.dragStarted = false;
                    if (!dragged(d)) {
                        // consider this the same as a 'click'
                        // (selection of a node)
                        if (clickEnabled()) {
                            selectCb.call(this, d);
                        }
                    }
                    d.fixed &= ~6;

                    // hook at the end of a drag gesture
                    if (dragEnabled()) {
                        atDragEnd.call(this, d);
                    }
                }
            });

        return drag;
    }


    function loadGlow(defs, r, g, b, id) {
        var glow = defs.append('filter')
            .attr('x', '-50%')
            .attr('y', '-50%')
            .attr('width', '200%')
            .attr('height', '200%')
            .attr('id', id);

        glow.append('feColorMatrix')
            .attr('type', 'matrix')
            .attr('values',
                '0 0 0 0  ' + r + ' ' +
                '0 0 0 0  ' + g + ' ' +
                '0 0 0 0  ' + b + ' ' +
                '0 0 0 1  0 ');

        glow.append('feGaussianBlur')
            .attr('stdDeviation', 3)
            .attr('result', 'coloredBlur');

        glow.append('feMerge').selectAll('feMergeNode')
            .data(['coloredBlur', 'SourceGraphic'])
            .enter().append('feMergeNode')
            .attr('in', String);
    }

    // deprecated -- we'll use something else to highlight instances for affinity
    function loadGlowDefs(defs) {
        loadGlow(defs, 0.0, 0.0, 0.7, 'blue-glow');
        loadGlow(defs, 1.0, 1.0, 0.3, 'yellow-glow');
    }

    // --- Ordinal scales for 7 values.
    // TODO: migrate these colors to the theme service.

    // Colors per Mojo-Design's color palette.. (version one)
    //               blue       red        dk grey    steel      lt blue    lt red     lt grey
    // var lightNorm = ['#5b99d2', '#d05a55', '#716b6b', '#7e9aa8', '#66cef6', '#db7773', '#aeada8' ],
    //     lightMute = ['#a8cceb', '#f1a7a7', '#b9b5b5', '#bdcdd5', '#a8e9fd', '#f8c9c9', '#d7d6d4' ],

    // Colors per Mojo-Design's color palette.. (version two)
    //               blue       lt blue    red        green      brown      teal       lime
    var lightNorm = ['#5b99d2', '#66cef6', '#d05a55', '#0f9d58', '#ba7941', '#3dc0bf', '#56af00' ],
        lightMute = ['#9ebedf', '#abdef5', '#d79a96', '#7cbe99', '#cdab8d', '#96d5d5', '#a0c96d' ],

        darkNorm  = ['#5b99d2', '#66cef6', '#d05a55', '#0f9d58', '#ba7941', '#3dc0bf', '#56af00' ],
        darkMute  = ['#9ebedf', '#abdef5', '#d79a96', '#7cbe99', '#cdab8d', '#96d5d5', '#a0c96d' ];


    var colors= {
        light: {
            norm: d3.scale.ordinal().range(lightNorm),
            mute: d3.scale.ordinal().range(lightMute)
        },
        dark: {
            norm: d3.scale.ordinal().range(darkNorm),
            mute: d3.scale.ordinal().range(darkMute)
        }
    };

    function cat7() {
        var tcid = 'd3utilTestCard';

        function getColor(id, muted, theme) {
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
                            f = getColor(id, muted, theme);
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
            getColor: getColor
        };
    }

    function translate(x, y) {
        if (fs.isA(x) && x.length === 2 && !y) {
            return 'translate(' + x[0] + ',' + x[1] + ')';
        }
        return 'translate(' + x + ',' + y + ')';
    }

    function scale(x, y) {
        return 'scale(' + x + ',' + y + ')';
    }

    function skewX(x) {
        return 'skewX(' + x + ')';
    }

    function rotate(deg) {
        return 'rotate(' + deg + ')';
    }

    function stripPx(s) {
        return s.replace(/px$/,'');
    }

    function safeId(s) {
        return s.replace(/[^a-z0-9]/gi, '-');
    }

    function makeVisible(el, b) {
        el.style('visibility', (b ? 'visible' : 'hidden'));
    }

    function isVisible(el) {
        return el.style('visibility') === 'visible';
    }

    function visible(el, x) {
        if (x === undefined) {
            return isVisible(el);
        } else {
            makeVisible(el, x);
        }
    }

    angular.module('onosSvg')
    .factory('SvgUtilService', ['$log', 'FnService',
        function (_$log_, _fs_) {
            $log = _$log_;
            fs = _fs_;

            return {
                createDragBehavior: createDragBehavior,
                loadGlowDefs: loadGlowDefs,
                cat7: cat7,
                translate: translate,
                scale: scale,
                skewX: skewX,
                rotate: rotate,
                stripPx: stripPx,
                safeId: safeId,
                visible: visible
            };
        }]);
}());
