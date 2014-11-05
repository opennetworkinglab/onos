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
        { text: 'All Layers', cb: showAllLayers },
        { text: 'Packet Only', cb: showPacketLayer },
        { text: 'Optical Only', cb: showOpticalLayer }
    ];

    // key bindings
    var keyDispatch = {
        Q: getUpdatedNetworkData,
        B: toggleBg,
        G: toggleLayout,
        L: cycleLabels,
        P: togglePorts,
        U: unpin
    };

    // state variables
    var network = {},
        selected = {},
        highlighted = null,
        hovered = null,
        viewMode = 'showAll',
        portLabelsOn = false;

    // D3 selections
    var svg,
        bgImg,
        topoG;

    // ==============================
    // For Debugging / Development

    var topoPrefix = 'json/topoTest_',
        lastFlavor = 4,
        topoBase = true,
        topoFlavor = 1;

    function nextTopo() {
        if (topoBase) {
            topoBase = false;
        } else {
            topoBase = true;
            topoFlavor = (topoFlavor === lastFlavor) ? 1 : topoFlavor + 1
        }
    }

    // TODO change this to return the live data URL
    function getTopoUrl() {
        var suffix = topoBase ? 'base' : topoFlavor;
        return topoPrefix + suffix + '.json';
    }

    // ==============================
    // Key Callbacks

    function getUpdatedNetworkData(view) {
        nextTopo();
        getNetworkData(view);
    }

    function toggleBg() {
        var vis = bgImg.style('visibility');
        bgImg.style('visibility', (vis === 'hidden') ? 'visible' : 'hidden');
    }

    function toggleLayout(view) {

    }

    function cycleLabels(view) {

    }

    function togglePorts(view) {

    }

    function unpin(view) {

    }

    // ==============================
    // Radio Button Callbacks

    function showAllLayers() {
//        network.node.classed('inactive', false);
//        network.link.classed('inactive', false);
//        d3.selectAll('svg .port').classed('inactive', false);
//        d3.selectAll('svg .portText').classed('inactive', false);
        // TODO ...
        console.log('showAllLayers()');
    }

    function showPacketLayer() {
        showAllLayers();
        // TODO ...
        console.log('showPacketLayer()');
    }

    function showOpticalLayer() {
        showAllLayers();
        // TODO ...
        console.log('showOpticalLayer()');
    }

    // ==============================
    // Private functions

    // set the size of the given element to that of the view
    function setSize(el, view) {
        el.attr({
            width: view.width(),
            height: view.height()
        });
    }


    function getNetworkData(view) {
        var url = getTopoUrl();

        // TODO ...

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
        setSize(svg, view);

        topoG = svg.append('g')
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
        view.setRadio(btnSet);
        view.setKeys(keyDispatch);

        getNetworkData(view);
    }

    function resize(view, ctx) {
        setSize(svg, view);
        setSize(bgImg, view);
    }


    // ==============================
    // View registration

    onos.ui.addView('topo', {
        preload: preload,
        load: load,
        resize: resize
    });

}(ONOS));
