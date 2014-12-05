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
 ONOS GUI -- Quick Help Layer

 Defines the key-map layer for the UI. Used to give user a list of
 key bindings; both global, and for the current view.

 @author Simon Hunt
 */

(function (onos){
    'use strict';

    // Config variables
    var w = '100%',
        h = '80%',
        fade = 500,
        vb = '-200 0 400 400';

    // layout configuration
    var pad = 10,
        offy = 45,
        sepYDelta = 20,
        colXDelta = 16,
        yTextSpc = 12,
        offDesc = 8;

    // State variables
    var data = [],
        yCount;

    // DOM elements and the like
    var qhdiv = d3.select('#quickhelp'),
        svg = qhdiv.select('svg'),
        pane, rect, items;

    // General functions
    function isA(a) { return $.isArray(a) ? a : null; }
    function isS(s) { return typeof s === 'string'; }

    function cap(s) {
        return s.replace(/^[a-z]/, function (m) { return m.toUpperCase(); });
    }

    var keyDisp = {
        equals: '=',
        dash: '-',
        slash: '/',
        backSlash: '\\',
        backQuote: '`',
        leftArrow: 'L-arrow',
        upArrow: 'U-arrow',
        rightArrow: 'R-arrow',
        downArrow: 'D-arrow'
    };

    function mkKeyDisp(id) {
        var v = keyDisp[id] || id;
        return cap(v);
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
                    transform: translate(xCount, 0)
                });

            c.forEach(function (j) {
                var k = j[0],
                    v = j[1];

                if (k !== '-') {
                    aggKey.append('text').text(k);

                    gcol.append('text').text(k)
                        .attr({
                            'class': 'key',
                            y: oy
                        });
                    gcol.append('text').text(v)
                        .attr({
                            'class': 'desc',
                            y: oy
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
                'class': 'qhrow'
            });

        entering.each(function (r, i) {
            var el = d3.select(this),
                sep = r.type === 'sep',
                dy;

            el.attr('transform', translate(0, yCount));

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
        items.attr('transform', translate(-paneW/2, -pad));
        rect.attr({
            width: paneW,
            height: paneH,
            transform: translate(-paneW/2-pad, 0)
        });

    }

    function translate(x, y) {
        return 'translate(' + x + ',' + y + ')';
    }

    function checkFmt(fmt) {
        // should be a single array of keys,
        // or array of arrays of keys (one per column).
        // return null if there is a problem.
        var a = isA(fmt),
            n = a && a.length,
            ns = 0,
            na = 0;

        if (n) {
            // it is an array which has some content
            a.forEach(function (d) {
                isA(d) && na++;
                isS(d) && ns++;
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
                a = isA(v),
                d = (a && a[1]);

            // '-' marks a separator; d is the description
            if (k === '-' || d) {
                b.push([mkKeyDisp(k), d]);
            }
        });
        return b;
    }

    function emptyRow() {
        return { type: 'row', data: []};
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
        addRow(isA(vfmt[0]) ? mkColumnarRow(vmap, vfmt) : mkMapRow(vmap, vfmt));
        addRow();
        addRow(mkArrRow(vgest));
    }


    function popBind(bindings) {
        pane = svg.append('g')
            .attr({
                class: 'help',
                opacity: 0
            });

        rect = pane.append('rect')
            .attr('rx', 8);

        pane.append('text')
            .text('Quick Help')
            .attr({
                class: 'title',
                dy: '1.2em',
                transform: translate(-pad,0)
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
            .duration(fade)
            .attr('opacity', o);
    }

    function addSvg() {
        svg = qhdiv.append('svg')
            .attr({
                width: w,
                height: h,
                viewBox: vb
            });
    }

    function removeSvg() {
        svg.transition()
            .delay(fade + 20)
            .remove();
    }

    function showQuickHelp(bindings) {
        svg = qhdiv.select('svg');
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

    onos.ui.addLib('quickHelp', {
        show: showQuickHelp,
        hide: hideQuickHelp
    });
}(ONOS));
