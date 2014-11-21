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
        fade = 750,
        vb = '-200 -200 400 400',
        xpad = 20,
        ypad = 10;

    // State variables
    var data = [];

    // DOM elements and the like
    var kmdiv = d3.select('#keymap');

    function computeBox(el) {
        var text = el.select('text'),
            box = text.node().getBBox();

        // center
        box.x = -box.width / 2;
        box.y = -box.height / 2;

        // add some padding
        box.x -= xpad;
        box.width += xpad * 2;
        box.y -= ypad;
        box.height += ypad * 2;

        return box;
    }

    function updateKeymap() {
        var items = svg.selectAll('.bindingItem')
            .data(data);

        var entering = items.enter()
            .append('g')
            .attr({
                class: 'bindingItem',
                opacity: 0
            })
            .transition()
            .duration(fade)
            .attr('opacity', 1);

        entering.each(function (d) {
            var el = d3.select(this),
                box;

            d.el = el;
            el.append('rect').attr({ rx: 10, ry: 10});
            el.append('text').text(d.label);
            box = computeBox(el);
            el.select('rect').attr(box);
        });

        items.exit()
            .transition()
            .duration(fade)
            .attr({ opacity: 0})
            .remove();
    }

    function clearFlash() {
        if (timer) {
            clearInterval(timer);
        }
        data = [];
        updateFeedback();
    }

    // for now, simply display some text feedback
    function flash(text) {
        // cancel old scheduled event if there was one
        if (timer) {
            clearInterval(timer);
        }
        timer = setInterval(function () {
            clearFlash();
        }, showFor);

        data = [{
            label: text
        }];
        updateFeedback();
    }

    // =====================================
    var svg = kmdiv.select('svg');

    function populateBindings(bindings) {
        svg.append('g')
            .attr({
                class: 'keyBindings',
                transform: 'translate(-200,-120)',
                opacity: 0
            })
            .transition()
            .duration(fade)
            .attr('opacity', 1);

        var g = svg.select('g');

        g.append('rect')
            .attr({
                width: 400,
                height: 240,
                rx: 8
            });

        g.append('text')
            .text('Key Bindings')
            .attr({
                x: 200,
                y: 0,
                dy: '1.4em',
                class: 'title'
            });

        // TODO: append .keyItems to rectangle
    }

    function fadeBindings() {
        svg.selectAll('g')
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
            console.log("SHOW KEY MAP");
        }
    }

    function hideKeyMap() {
        svg = kmdiv.select('svg');
        if (!svg.empty()) {
            fadeBindings();
            removeSvg();
            console.log("HIDE KEY MAP");
            return true;
        }
        return false;
    }

    onos.ui.addLib('keymap', {
        show: showKeyMap,
        hide: hideKeyMap
    });
}(ONOS));
