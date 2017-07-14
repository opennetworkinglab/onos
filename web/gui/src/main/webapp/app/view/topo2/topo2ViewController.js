/*
 * Copyright 2016-present Open Networking Foundation
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
 ONOS GUI -- View Controller.
 A base class for view controllers to extend from
 */

(function () {
    'use strict';

    var flash, ps;

    function ViewController(options) {
        this.initialize.apply(this, arguments);
    }

    ViewController.prototype = {

        id: null,
        displayName: 'View',

        initialize: function () {
            this.name = this.displayName.toLowerCase().replace(/ /g, '_');
            this.prefs = {
                visible: this.name + '_visible',
            };
        },
        appendElement: function (parent, node) {
            var el = d3.select('#' + this.id);
            if (el.empty()) {
                 return d3.select(parent).append(node).attr('id', this.id);
            }
            return el;
        },
        node: function () {
            return d3.select('#' + this.id);
        },
        enabled: function () {
            return ps.getPrefs('topo2_prefs')[this.prefs.visible];
        },
        isVisible: function () {
            return this.node().style('visibility') === 'visible';
        },
        hide: function () {
            var node = this.node();

            if (this.isVisible()) {
                node
                    .transition()
                    .duration(400)
                    .style('opacity', 0)
                    .each('end', function () {
                        node.style('visibility', 'hidden');
                    });
            }
        },
        show: function () {
            var node = this.node();

            if (!this.isVisible()) {
                node
                    .style('visibility', 'visible')
                    .transition()
                    .duration(400)
                    .style('opacity', 1);
            }
        },
        toggle: function () {
            var on = !Boolean(this.isVisible()),
                verb = on ? 'Show' : 'Hide';

            on ? this.show() : this.hide();
            flash.flash(verb + ' ' + this.displayName);
            this.updatePrefState(this.prefs.visible, on);
        },
        lookupPrefState: function (key) {
            // Return 0 if not defined
            return ps.getPrefs('topo2_prefs')[key] || 0;
        },
        updatePrefState: function (key, value) {
            var state = ps.getPrefs('topo2_prefs');
            state[key] = value ? 1 : 0;
            ps.setPrefs('topo2_prefs', state);
        },
    };

    angular.module('ovTopo2')
        .factory('Topo2ViewController', [
            'FnService', 'FlashService', 'PrefsService',
            function (fn, _flash_, _ps_) {

                flash = _flash_;
                ps = _ps_;

                ViewController.extend = fn.extend;
                return ViewController;
            },
        ]);
})();
