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

    var ps;

    var Panel = function (id, options) {
        this.id = id;
        this.p = ps.createPanel(this.id, options);
        this.setup();

        this.p.show();
    };

    Panel.prototype = {
        setup: function () {
            var panel = this.p;
            panel.empty();

            panel.append('div').classed('header', true);
            panel.append('div').classed('body', true);
            panel.append('div').classed('footer', true);

            this.header = panel.el().select('.header');
            this.body = panel.el().select('.body');
            this.footer = panel.el().select('.body');
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
        destory: function () {
            ps.destroy(this.id);
        }
    };

    angular.module('ovTopo2')
    .factory('Topo2PanelService', ['PanelService',
        function (_ps_) {
            ps = _ps_;
            return Panel;
        }
    ]);

})();
