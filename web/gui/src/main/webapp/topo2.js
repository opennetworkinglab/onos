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
 ONOS network topology viewer - version 1.1

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    // configuration data
    var config = {
        useLiveData: false,
        debugOn: false,
        debug: {
            showNodeXY: false,
            showKeyHandler: true
        },
        options: {
            layering: true,
            collisionPrevention: true,
            showBackground: true
        },
        backgroundUrl: 'img/us-map.png',
        data: {
            live: {
                jsonUrl: 'rs/topology/graph',
                detailPrefix: 'rs/topology/graph/',
                detailSuffix: ''
            },
            fake: {
                jsonUrl: 'json/network2.json',
                detailPrefix: 'json/',
                detailSuffix: '.json'
            }
        },
        iconUrl: {
            device: 'img/device.png',
            host: 'img/host.png',
            pkt: 'img/pkt.png',
            opt: 'img/opt.png'
        },
        force: {
            marginLR: 20,
            marginTB: 20,
            translate: function() {
                return 'translate(' +
                    config.force.marginLR + ',' +
                    config.force.marginTB + ')';
            }
        }
    };

    // radio buttons
    var btnSet = [
            { id: 'showAll', text: 'All Layers' },
            { id: 'showPkt', text: 'Packet Only' },
            { id: 'showOpt', text: 'Optical Only' }
        ];

    // state variables
    var svg,
        bgImg,
        network = {},
        selected = {},
        highlighted = null,
        hovered = null,
        viewMode = 'showAll',
        portLabelsOn = false;


    // ==============================
    // Private functions

    // set the size of the SVG layer (or other element) to that of the view
    function setSize(view, el) {
        var thing = el || svg;
        thing.attr({
            width: view.width(),
            height: view.height()
        });
    }

    function doRadio(view, id) {
        showAllLayers();
        if (id === 'showPkt') {
            showPacketLayer();
        } else if (id === 'showOpt') {
            showOpticalLayer();
        }
    }

    function showAllLayers() {
//        network.node.classed('inactive', false);
//        network.link.classed('inactive', false);
//        d3.selectAll('svg .port').classed('inactive', false);
//        d3.selectAll('svg .portText').classed('inactive', false);
        alert('show all layers');
    }

    function showPacketLayer() {
        alert('show packet layer');
    }

    function showOpticalLayer() {
        alert('show optical layer');
    }

    // ==============================
    // View life-cycle callbacks

    function preload(view, ctx) {
        var w = view.width(),
            h = view.height(),
            idBg = view.uid('bg'),
            showBg = config.options.showBackground ? 'visible' : 'hidden';

        // NOTE: view.$div is a D3 selection of the view's div
        svg = view.$div.append('svg');
        setSize(view);
        svg.append('g')
            .attr('transform', config.force.translate());

        // load the background image
        bgImg = svg.append('svg:image')
            .attr({
                id: idBg,
                width: w,
                height: h,
                'xlink:href': config.backgroundUrl
            })
            .style({
                visibility: showBg
            });
    }


    function load(view, ctx) {
        view.setRadio(btnSet, doRadio);

    }

    function resize(view, ctx) {
        setSize(view);
        setSize(view, bgImg);
    }


    // ==============================
    // View registration

    onos.ui.addView('topo', {
        preload: preload,
        load: load,
        resize: resize
    });

}(ONOS));
