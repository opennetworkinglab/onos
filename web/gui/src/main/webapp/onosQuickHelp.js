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

    // API's
    var api = onos.api;

    // Config variables
    var w = '100%',
        h = '80%',
        fade = 500,
        vb = '-200 0 400 400';

    // State variables
    var data = [];

    // DOM elements and the like
    var qhdiv = d3.select('#quickhelp'),
        svg = qhdiv.select('svg'),
        pane,
        rect,
        items,
        keyAgg;

    // General functions
    function isA(a) {
        return $.isArray(a) ? a : null;
    }

    var keyDisp = {
        equals: '=',
        dash: '-',
        slash: '/',
        backQuote: '`',
        leftArrow: 'L-arrow',
        upArrow: 'U-arrow',
        rightArrow: 'R-arrow',
        downArrow: 'D-arrow'
    };

    function cap(s) {
        return s.replace(/^[a-z]/, function (m) { return m.toUpperCase(); });
    }

    function mkKeyDisp(id) {
        var v = keyDisp[id] || id;
        return cap(v);
    }

    // layout configuration
    var pad = 8,
        offy = 45,
        dy = 10,
        offDesc = 8;

    // D3 magic
    function updateKeyItems() {
        var keyItems = items.selectAll('.keyItem')
            .data(data);

        var entering = keyItems.enter()
            .append('g')
            .attr({
                id: function (d) { return d.id; },
                class: 'keyItem'
            });

        entering.each(function (d, i) {
            var el = d3.select(this),
                y = offy + dy * i;

            if (d.id[0] === '_') {
                el.append('line')
                    .attr({ x1: 0, y1: y, x2: 1, y2: y});
            } else {
                el.append('text')
                    .text(d.key)
                    .attr({
                        class: 'key',
                        x: 0,
                        y: y
                    });
                // NOTE: used for sizing column width...
                keyAgg.append('text').text(d.key).attr('class', 'key');

                el.append('text')
                    .text(d.desc)
                    .attr({
                        class: 'desc',
                        x: offDesc,
                        y: y
                    });
            }
        });

        var kbox = keyAgg.node().getBBox();
        items.selectAll('.desc').attr('x', kbox.width + offDesc);

        var box = items.node().getBBox(),
            paneW = box.width + pad * 2,
            paneH = box.height + offy;

        items.selectAll('line').attr('x2', box.width);
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

    function aggregateData(bindings) {
        var hf = '_helpFormat',
            gmap = d3.map(bindings.globalKeys),
            vmap = d3.map(bindings.viewKeys),
            fmt = vmap.get(hf),
            vgest = bindings.viewGestures,
            gkeys = gmap.keys(),
            vkeys,
            sep = 0;

        // filter out help format entry
        vmap.remove(hf);
        vkeys = vmap.keys(),

        gkeys.sort();
        vkeys.sort();

        data = [];
        gkeys.forEach(function (k) {
            addItem('glob', k, gmap.get(k));
        });
        addItem('sep');
        vkeys.forEach(function (k) {
            addItem('view', k, vmap.get(k));
        });
        addItem('sep');
        vgest.forEach(function (g) {
            if (g.length === 2) {
                addItem('gest', g[0], g[1]);
            }
        });


        function addItem(type, k, d) {
            var id = type + '-' + k,
                a = isA(d),
                desc = a && a[1];

            if (type === 'sep') {
                data.push({
                    id: '_' + sep++,
                    type: type
                });
            } else if (type === 'gest') {
                data.push({
                    id: id,
                    type: type,
                    key: k,
                    desc: d
                });
            } else if (desc) {
                data.push(
                    {
                        id: id,
                        type: type,
                        key: mkKeyDisp(k),
                        desc: desc
                    }
                );
            }
        }
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
        keyAgg = pane.append('g').style('visibility', 'hidden');

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
