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
 ONOS GUI -- Keymap Layer

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
        vb = '-220 -220 440 440',
        paneW = 400,
        paneH = 280,
        offy = 65,
        dy = 14,
        offKey = 40,
        offDesc = offKey + 50,
        lineW = paneW - (2*offKey);

    // State variables
    var data = [];

    // DOM elements and the like
    var kmdiv = d3.select('#keymap');

    function isA(a) {
        return $.isArray(a) ? a : null;
    }


    var svg = kmdiv.select('svg'),
        pane;

    function updateKeyItems() {
        var items = pane.selectAll('.keyItem')
            .data(data);

        var entering = items.enter()
            .append('g')
            .attr({
                id: function (d) { return d.id; },
                class: 'keyItem'
            });

        entering.each(function (d, i) {
            var el = d3.select(this),
                y = offy + dy * i;

            if (d.id === '_') {
                el.append('line')
                    .attr({
                        class: 'sep',
                        x1: offKey,
                        y1: y,
                        x2: offKey + lineW,
                        y2: y
                    });
            } else {
                el.append('text')
                    .text(d.key)
                    .attr({
                        class: 'key',
                        x: offKey,
                        y: y
                    });

                el.append('text')
                    .text(d.desc)
                    .attr({
                        class: 'desc',
                        x: offDesc,
                        y: y
                    });
            }
        });
    }

    function aggregateData(bindings) {
        var gmap = d3.map(bindings.globalKeys),
            vmap = d3.map(bindings.viewKeys),
            gkeys = gmap.keys(),
            vkeys = vmap.keys();

        gkeys.sort();
        vkeys.sort();

        data = [];
        gkeys.forEach(function (k) {
            addItem('global', k, gmap.get(k));
        });
        addItem('separator');
        vkeys.forEach(function (k) {
            addItem('view', k, vmap.get(k));
        });

        function addItem(type, k, d) {
            var id = type + '-' + k,
                a = isA(d),
                desc = a && a[1];
            if (desc) {
                data.push(
                    {
                        id: id,
                        type: type,
                        key: k,
                        desc: desc
                    }
                );
            } else if (type === 'separator') {
                data.push({
                    id: '_',
                    type: type
                });
            }
        }

    }

    function populateBindings(bindings) {
        svg.append('g')
            .attr({
                class: 'keyBindings',
                transform: 'translate(-200,-200)',
                opacity: 0
            })
            .transition()
            .duration(fade)
            .attr('opacity', 1);

        pane = svg.select('g');

        pane.append('rect')
            .attr({
                width: paneW,
                height: paneH,
                rx: 8
            });

        pane.append('text')
            .text('Keyboard Shortcuts')
            .attr({
                x: 200,
                y: 0,
                dy: '1.4em',
                class: 'title'
            });

        aggregateData(bindings);
        updateKeyItems();
    }

    function fadeBindings() {
        svg.selectAll('g.keyBindings')
            .transition()
            .duration(fade)
            .attr('opacity', 0);
    }

    function addSvg() {
        svg = kmdiv.append('svg')
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

    function showKeyMap(bindings) {
        svg = kmdiv.select('svg');
        if (svg.empty()) {
            addSvg();
            populateBindings(bindings);
        } else {
            hideKeyMap();
        }
    }

    function hideKeyMap() {
        svg = kmdiv.select('svg');
        if (!svg.empty()) {
            fadeBindings();
            removeSvg();
            return true;
        }
        return false;
    }

    onos.ui.addLib('keymap', {
        show: showKeyMap,
        hide: hideKeyMap
    });
}(ONOS));
