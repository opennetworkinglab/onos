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
        useLiveData: true,
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
            note: 'node.class or link.class is used to differentiate',
            linkDistance: {
                infra: 200,
                host: 40
            },
            linkStrength: {
                infra: 1.0,
                host: 1.0
            },
            charge: {
                device: -400,
                host: -100
            },
            pad: 20,
            translate: function() {
                return 'translate(' +
                    config.force.pad + ',' +
                    config.force.pad + ')';
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
        topoG,
        nodeG,
        linkG,
        node,
        link;

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

    // set the size of the given element to that of the view (reduced if padded)
    function setSize(el, view, pad) {
        var padding = pad ? pad * 2 : 0;
        el.attr({
            width: view.width() - padding,
            height: view.height() - padding
        });
    }

    function getNetworkData(view) {
        var url = getTopoUrl();

        console.log('Fetching JSON: ' + url);
        d3.json(url, function(err, data) {
            if (err) {
                view.dataLoadError(err, url);
            } else {
                network.data = data;
                drawNetwork(view);
            }
        });
    }

    function drawNetwork(view) {
        preprocessData(view);
        updateLayout(view);
    }

    function preprocessData(view) {
        var w = view.width(),
            h = view.height(),
            hDevice = h * 0.6,
            hHost = h * 0.3,
            data = network.data,
            deviceLayout = computeInitLayout(w, hDevice, data.devices.length),
            hostLayout = computeInitLayout(w, hHost, data.hosts.length);

        network.lookup = {};
        network.nodes = [];
        network.links = [];
        // we created new arrays, so need to set the refs in the force layout
        network.force.nodes(network.nodes);
        network.force.links(network.links);

        // let's just start with the nodes

        // note that both 'devices' and 'hosts' get mapped into the nodes array
        function makeNode(d, cls, layout) {
            var node = {
                    id: d.id,
                    labels: d.labels,
                    class: cls,
                    icon: cls,
                    type: d.type,
                    x: layout.x(),
                    y: layout.y()
                };
            network.lookup[d.id] = node;
            network.nodes.push(node);
        }

        // first the devices...
        network.data.devices.forEach(function (d) {
            makeNode(d, 'device', deviceLayout);
        });

        // then the hosts...
        network.data.hosts.forEach(function (d) {
            makeNode(d, 'host', hostLayout);
        });

        // TODO: process links
    }

    function computeInitLayout(w, h, n) {
        var maxdw = 60,
            compdw, dw, ox, layout;

        if (n < 2) {
            layout = { ox: w/2, dw: 0 }
        } else {
            compdw = (0.8 * w) / (n - 1);
            dw = Math.min(maxdw, compdw);
            ox = w/2 - ((n - 1)/2 * dw);
            layout = { ox: ox, dw: dw }
        }

        layout.i = 0;

        layout.x = function () {
            var x = layout.ox + layout.i*layout.dw;
            layout.i++;
            return x;
        };

        layout.y = function () {
            return h;
        };

        return layout;
    }

    function linkId(d) {
        return d.source.id + '~' + d.target.id;
    }

    function nodeId(d) {
        return d.id;
    }

    function updateLayout(view) {
        link = link.data(network.force.links(), linkId);
        link.enter().append('line')
            .attr('class', 'link');
        link.exit().remove();

        node = node.data(network.force.nodes(), nodeId);
        node.enter().append('circle')
            .attr('id', function (d) { return 'nodeId-' + d.id; })
            .attr('class', function (d) { return 'node'; })
            .attr('r', 12);

        network.force.start();
    }


    function tick() {
        node.attr({
            cx: function(d) { return d.x; },
            cy: function(d) { return d.y; }
        });

        link.attr({
            x1: function (d) { return d.source.x; },
            y1: function (d) { return d.source.y; },
            x2: function (d) { return d.target.x; },
            y2: function (d) { return d.target.y; }
        });
    }

    // ==============================
    // View life-cycle callbacks

    function preload(view, ctx) {
        var w = view.width(),
            h = view.height(),
            idBg = view.uid('bg'),
            showBg = config.options.showBackground ? 'visible' : 'hidden',
            fcfg = config.force,
            fpad = fcfg.pad,
            forceDim = [w - 2*fpad, h - 2*fpad];

        // NOTE: view.$div is a D3 selection of the view's div
        svg = view.$div.append('svg');
        setSize(svg, view);

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

        // group for the topology
        topoG = svg.append('g')
            .attr('transform', fcfg.translate());

        // subgroups for links and nodes
        linkG = topoG.append('g').attr('id', 'links');
        nodeG = topoG.append('g').attr('id', 'nodes');

        // selection of nodes and links
        link = linkG.selectAll('.link');
        node = nodeG.selectAll('.node');

        // set up the force layout
        network.force = d3.layout.force()
            .size(forceDim)
            .nodes(network.nodes)
            .links(network.links)
            .charge(function (d) { return fcfg.charge[d.class]; })
            .linkDistance(function (d) { return fcfg.linkDistance[d.class]; })
            .linkStrength(function (d) { return fcfg.linkStrength[d.class]; })
            .on('tick', tick);
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
