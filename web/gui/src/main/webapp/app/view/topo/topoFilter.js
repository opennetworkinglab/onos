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
 ONOS GUI -- Topology layer filtering Module.
 Provides functionality to visually differentiate between the packet and
 optical layers of the topology.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, tps, tts;

    // api to topoForce
    var api;
    /*
     node()     // get ref to D3 selection of nodes
     link()     // get ref to D3 selection of links
     */

    // which "layer" a particular item "belongs to"
    var layerLookup = {
        host: {
            endstation: 'pkt', // default, if host event does not define type
            router:     'pkt',
            bgpSpeaker: 'pkt'
        },
        device: {
            switch: 'pkt',
            roadm: 'opt'
        },
        link: {
            hostLink: 'pkt',
            direct: 'pkt',
            indirect: '',
            tunnel: '',
            optical: 'opt'
        }
    };

    var idPrefix = 'topo-rb-';

    var dispatch = {
            all: function () { suppressLayers(false); },
            pkt: function () { showLayer('pkt'); },
            opt: function () { showLayer('opt'); }
        },
        filterButtons = [
            { text: 'All Layers', id: 'all' },
            { text: 'Packet Only', id: 'pkt' },
            { text: 'Optical Only', id: 'opt' }
        ],
        btnG,
        btnDef = {},
        targetDiv;


    function injectButtons(div) {
        targetDiv = div;

        btnG = div.append('div').attr('id', 'topo-radio-group');

        filterButtons.forEach(function (btn, i) {
            var bid = btn.id,
                txt = btn.text,
                uid = idPrefix + bid,
                button = btnG.append('span')
                    .attr({
                        id: uid,
                        'class': 'radio'
                    })
                    .text(txt);
            btnDef[uid] = btn;

            if (i === 0) {
                button.classed('active', true);
                btnG.selected = bid;
            }
        });

        btnG.selectAll('span')
            .on('click', function () {
               var button = d3.select(this),
                   uid = button.attr('id'),
                   btn = btnDef[uid],
                   act = button.classed('active');

                if (!act) {
                    btnG.selectAll('span').classed('active', false);
                    button.classed('active', true);
                    btnG.selected = btn.id;
                    clickAction(btn.id);
                }
            });
    }

    function clickAction(which) {
        dispatch[which]();
    }

    function selected() {
        return btnG ? btnG.selected : '';
    }

    function inLayer(d, layer) {
        var type = d.class === 'link' ? d.type() : d.type,
            look = layerLookup[d.class],
            lyr = look && look[type];
        return lyr === layer;
    }

    function unsuppressLayer(which) {
        api.node().each(function (d) {
            var node = d.el;
            if (inLayer(d, which)) {
                node.classed('suppressed', false);
            }
        });

        api.link().each(function (d) {
            var link = d.el;
            if (inLayer(d, which)) {
                link.classed('suppressed', false);
            }
        });
    }

    function suppressLayers(b) {
        api.node().classed('suppressed', b);
        api.link().classed('suppressed', b);
//        d3.selectAll('svg .port').classed('inactive', false);
//        d3.selectAll('svg .portText').classed('inactive', false);
    }

    function showLayer(which) {
        suppressLayers(true);
        unsuppressLayer(which);
    }

    // === -----------------------------------------------------
    // === MODULE DEFINITION ===

    angular.module('ovTopo')
        .factory('TopoFilterService',
        ['$log', 'FnService',
            'FlashService',
            'TopoPanelService',
            'TopoTrafficService',

            function (_$log_, _fs_, _flash_, _tps_, _tts_) {
                $log = _$log_;
                fs = _fs_;
                flash = _flash_;
                tps = _tps_;
                tts = _tts_;

                function initFilter(_api_, div) {
                    api = _api_;
                    injectButtons(div);
                }

                function destroyFilter() {
                    targetDiv.select('#topo-radio-group').remove();
                    btnG = null;
                    btnDef = {};
                }

                return {
                    initFilter: initFilter,
                    destroyFilter: destroyFilter,

                    clickAction: clickAction,
                    selected: selected,
                    inLayer: inLayer
                };
            }]);
}());
