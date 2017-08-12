/*
 * Copyright 2015-present Open Networking Foundation
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
 ONOS GUI -- Layer -- Quick Help Service

 Provides a mechanism to display key bindings and mouse gesture notes.
 */
(function () {
    'use strict';

    // injected references
    var $log, fs, sus, ls;

    // configuration
    var defaultSettings = {
            fade: 500,
        },
        w = '100%',
        h = '80%',
        vbox = '-200 0 400 400',
        pad = 10,
        offy = 45,
        sepYDelta = 20,
        colXDelta = 16,
        yTextSpc = 12,
        offDesc = 8;

    // internal state
    var settings,
        data = [],
        yCount;

    // DOM elements
    var qhdiv, svg, pane, rect, items;

    // key-logical-name to key-display lookup..
    var keyDisp = {
        equals: '=',
        slash: '/',
        backSlash: '\\',
        backQuote: '`',
        leftArrow: 'L-arrow',
        upArrow: 'U-arrow',
        rightArrow: 'R-arrow',
        downArrow: 'D-arrow',
    };

    // list of needed bindings to use in aggregateData
    var neededBindings = [
        'globalKeys', 'globalFormat', 'viewKeys', 'viewGestures',
    ];

    // ===========================================
    // === Function Definitions ===

    function mkKeyDisp(id) {
        var v = keyDisp[id] || id;
        return fs.cap(v);
    }

    function addSeparator(el, i) {
        var y = sepYDelta/2 - 5;
        el.append('line')
            .attr({ 'class': 'qhrowsep', x1: 0, y1: y, x2: 0, y2: y });
    }

    function addContent(el, data, ri) {
        var xCount = 0,
            clsPfx = 'qh-r' + ri + '-c';

        function addColumn(el, c, i) {
            var cls = clsPfx + i,
                oy = 0,
                aggKey = el.append('g').attr('visibility', 'hidden'),
                gcol = el.append('g').attr({
                    'class': cls,
                    transform: sus.translate(xCount, 0),
                });

            c.forEach(function (j) {
                var k = j[0],
                    v = j[1];

                if (k !== '-') {
                    aggKey.append('text').text(k);

                    gcol.append('text').text(k)
                        .attr({
                            'class': 'key',
                            y: oy,
                        });
                    gcol.append('text').text(v)
                        .attr({
                            'class': 'desc',
                            y: oy,
                        });
                }

                oy += yTextSpc;
            });

            // adjust position of descriptions, based on widest key
            var kbox = aggKey.node().getBBox(),
                ox = kbox.width + offDesc;
            gcol.selectAll('.desc').attr('x', ox);
            aggKey.remove();

            // now update x-offset for next column
            var bbox = gcol.node().getBBox();
            xCount += bbox.width + colXDelta;
        }

        data.forEach(function (d, i) {
            addColumn(el, d, i);
        });

        // finally, return the height of the row..
        return el.node().getBBox().height;
    }

    function updateKeyItems() {
        var rows = items.selectAll('.qhRow').data(data);

        yCount = offy;

        var entering = rows.enter()
            .append('g')
            .attr({
                'class': 'qhrow',
            });

        entering.each(function (r, i) {
            var el = d3.select(this),
                sep = r.type === 'sep',
                dy;

            el.attr('transform', sus.translate(0, yCount));

            if (sep) {
                addSeparator(el, i);
                yCount += sepYDelta;
            } else {
                dy = addContent(el, r.data, i);
                yCount += dy;
            }
        });

        // size the backing rectangle
        var ibox = items.node().getBBox(),
            paneW = ibox.width + pad * 2,
            paneH = ibox.height + offy;

        items.selectAll('.qhrowsep').attr('x2', ibox.width);
        items.attr('transform', sus.translate(-paneW/2, -pad));
        rect.attr({
            width: paneW,
            height: paneH,
            transform: sus.translate(-paneW/2-pad, 0),
        });

    }

    function checkFmt(fmt) {
        // should be a single array of keys,
        // or array of arrays of keys (one per column).
        // return null if there is a problem.
        var a = fs.isA(fmt),
            n = a && a.length,
            ns = 0,
            na = 0;

        if (n) {
            // it is an array which has some content
            a.forEach(function (d) {
                fs.isA(d) && na++;
                fs.isS(d) && ns++;
            });
            if (na === n || ns === n) {
                // all arrays or all strings...
                return a;
            }
        }
        return null;
    }

    function buildBlock(map, fmt) {
        var b = [];
        fmt.forEach(function (k) {
            var v = map.get(k),
                a = fs.isA(v),
                d = (a && a[1]),
                dfn = fs.isF(d),
                dval = (dfn && dfn()) || d;

            // '-' marks a separator; d is the description
            if (k === '-' || dval) {
                b.push([mkKeyDisp(k), dval]);
            }
        });
        return b;
    }

    function emptyRow() {
        return { type: 'row', data: [] };
    }

    function mkArrRow(fmt) {
        var d = emptyRow();
        d.data.push(fmt);
        return d;
    }

    function mkColumnarRow(map, fmt) {
        var d = emptyRow();
        fmt.forEach(function (a) {
            d.data.push(buildBlock(map, a));
        });
        return d;
    }

    function mkMapRow(map, fmt) {
        var d = emptyRow();
        d.data.push(buildBlock(map, fmt));
        return d;
    }

    function addRow(row) {
        var d = row || { type: 'sep' };
        data.push(d);
    }

    function aggregateData(bindings) {
        var hf = '_helpFormat',
            gmap = d3.map(bindings.globalKeys),
            gfmt = bindings.globalFormat,
            vmap = d3.map(bindings.viewKeys),
            vgest = bindings.viewGestures,
            vfmt, vkeys;

        // filter out help format entry
        vfmt = checkFmt(vmap.get(hf));
        vmap.remove(hf);

        // if bad (or no) format, fallback to sorted keys
        if (!vfmt) {
            vkeys = vmap.keys();
            vfmt = vkeys.sort();
        }

        data = [];

        addRow(mkMapRow(gmap, gfmt));
        addRow();
        addRow(fs.isA(vfmt[0]) ? mkColumnarRow(vmap, vfmt) : mkMapRow(vmap, vfmt));
        addRow();
        addRow(mkArrRow(vgest));
    }

    function qhlionTitle() {
        var lion = ls.bundle('core.fw.QuickHelp');
        return lion('qh_title');
    }

    function popBind(bindings) {
        pane = svg.append('g')
            .attr({
                class: 'help',
                opacity: 0,
            });

        rect = pane.append('rect')
            .attr('rx', 8);

        pane.append('text')
            .text(qhlionTitle())
            .attr({
                class: 'title',
                dy: '1.2em',
                transform: sus.translate(-pad, 0),
            });

        items = pane.append('g');
        aggregateData(bindings);
        updateKeyItems();

        _fade(1);
    }

    function fadeBindings() {
        _fade(0);
    }

    function _fade(o) {
        svg.selectAll('g.help')
            .transition()
            .duration(settings.fade)
            .attr('opacity', o);
    }

    function addSvg() {
        svg = qhdiv.append('svg')
            .attr({
                width: w,
                height: h,
                viewBox: vbox,
            });
    }

    function removeSvg() {
        svg.transition()
            .delay(settings.fade + 20)
            .remove();
    }

    function goodBindings(bindings) {
        var warnPrefix = 'Quickhelp Service: showQuickHelp(), ';
        if (!bindings || !fs.isO(bindings) || fs.isEmptyObject(bindings)) {
            $log.warn(warnPrefix + 'invalid bindings object');
            return false;
        }
        if (!(neededBindings.every(function (key) { return key in bindings; }))) {
            $log.warn(
                warnPrefix +
                'needed bindings for help panel not provided:',
                neededBindings
            );
            return false;
        }
        return true;
    }

    // ===========================================
    // === Module Definition ===

    angular.module('onosLayer')
    .factory('QuickHelpService',
        ['$log', 'FnService', 'SvgUtilService', 'LionService',

        function (_$log_, _fs_, _sus_, _ls_) {
            $log = _$log_;
            fs = _fs_;
            sus = _sus_;
            ls = _ls_;

            function initQuickHelp(opts) {
                settings = angular.extend({}, defaultSettings, fs.isO(opts));
                qhdiv = d3.select('#quickhelp');
            }

            function showQuickHelp(bindings) {
                svg = qhdiv.select('svg');
                if (!goodBindings(bindings)) {
                    return null;
                }
                if (svg.empty()) {
                    addSvg();
                    popBind(bindings);
                } else {
                    hideQuickHelp();
                }
            }

            function hideQuickHelp() {
                svg = qhdiv.select('svg');
                if (!svg.empty()) {
                    fadeBindings();
                    removeSvg();
                    return true;
                }
                return false;
            }

            return {
                initQuickHelp: initQuickHelp,
                showQuickHelp: showQuickHelp,
                hideQuickHelp: hideQuickHelp,
            };
        }]);

}());
