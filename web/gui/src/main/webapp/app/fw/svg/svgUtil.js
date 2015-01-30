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
 ONOS GUI -- SVG -- Util Service
 */

/*
 The SVG Util Service provides a miscellany of utility functions.
 */

(function () {
    'use strict';

    // injected references
    var $log, fs;

    angular.module('onosSvg')
        .factory('SvgUtilService', ['$log', 'FnService',
        function (_$log_, _fs_) {
            $log = _$log_;
            fs = _fs_;

            function createDragBehavior(force, selectCb, atDragEnd,
                                        dragEnabled, clickEnabled) {
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
                                    selectCb(d, this);
                                    // TODO: set 'this' context instead of param
                                }
                            }
                            d.fixed &= ~6;

                            // hook at the end of a drag gesture
                            if (dragEnabled()) {
                                atDragEnd(d, this);
                                // TODO: set 'this' context instead of param
                            }
                        }
                    });

                return drag;            }

            function loadGlow() {
                $log.warn('SvgUtilService: loadGlow -- To Be Implemented');
            }

            function cat7() {
                $log.warn('SvgUtilService: cat7 -- To Be Implemented');
            }

            function translate(x, y) {
                if (fs.isA(x) && x.length === 2 && !y) {
                    return 'translate(' + x[0] + ',' + x[1] + ')';
                }
                return 'translate(' + x + ',' + y + ')';
            }

            function stripPx(s) {
                return s.replace(/px$/,'');
            }

            return {
                createDragBehavior: createDragBehavior,
                loadGlow: loadGlow,
                cat7: cat7,
                translate: translate,
                stripPx: stripPx
            };
        }]);
}());
