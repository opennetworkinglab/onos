/*
 * Copyright 2016-present Open Networking Laboratory
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
 ONOS GUI -- Topology Layout Module.
 Module that contains the d3.force.layout logic
 */

(function () {
    'use strict';

    // Injected Services
    var ps;

    var panel = {
        initialize: function (id, options) {
            this.id = id;
            this.el = ps.createPanel(id, options);
            this.setup();

            if (options.show) {
                this.el.show();
            }
        },
        setup: function () {
            this.el.empty();
            this.header = this.el.append('div').classed('header', true);
            this.body = this.el.append('div').classed('body', true);
            this.footer = this.el.append('div').classed('footer', true);
        },
        appendToHeader: function (x) {
            return this.header.append(x);
        },
        appendToBody: function (x) {
            return this.body.append(x);
        },
        appendToFooter: function (x) {
            return this.footer.append(x);
        },
        emptyRegions: function () {
            this.header.selectAll("*").remove();
            this.body.selectAll("*").remove();
            this.footer.selectAll("*").remove();
        },
        destroy: function () {
            ps.destroyPanel(this.id);
        },
        isVisible: function () {
            return this.el.isVisible();
        }
    };

    angular.module('ovTopo2')
    .factory('Topo2PanelService', [
        'Topo2UIView', 'PanelService',
        function (View, _ps_) {

            ps = _ps_;

            return View.extend(panel);
        }
    ]);

})();
