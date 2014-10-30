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
 ONOS network topology viewer - PoC version 1.0

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    // reference to the framework api
    var api = onos.api;

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
            collisionPrevention: true
        },
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
        mastHeight: 36,
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
                device: -800,
                host: -1000
            },
            ticksWithoutCollisions: 50,
            marginLR: 20,
            marginTB: 20,
            translate: function() {
                return 'translate(' +
                    config.force.marginLR + ',' +
                    config.force.marginTB + ')';
            }
        },
        labels: {
            imgPad: 16,
            padLR: 8,
            padTB: 6,
            marginLR: 3,
            marginTB: 2
        },
        icons: {
            w: 32,
            h: 32,
            xoff: -12,
            yoff: -8
        },
        constraints: {
            ypos: {
                host: 0.05,
                switch: 0.3,
                roadm: 0.7
            }
        },
        hostLinkWidth: 1.0,
        hostRadius: 7,
        mouseOutTimerDelayMs: 120
    };

    // state variables
    var view = {},
        network = {},
        selected = {},
        highlighted = null,
        hovered = null,
        viewMode = 'showAll';


    function debug(what) {
        return config.debugOn && config.debug[what];
    }

    function urlData() {
        return config.data[config.useLiveData ? 'live' : 'fake'];
    }

    function networkJsonUrl() {
        return urlData().jsonUrl;
    }

    function detailJsonUrl(id) {
        var u = urlData(),
            encId = config.useLiveData ? encodeURIComponent(id)
                : id.replace(/[^a-z0-9]/gi, '_');
        return u.detailPrefix + encId + u.detailSuffix;
    }


    // load the topology view of the network
    function loadNetworkView() {
        // Hey, here I am, calling something on the ONOS api:
        api.printTime();

        resize();

        // go get our network data from the server...
        var url = networkJsonUrl();
        d3.json(url , function (err, data) {
            if (err) {
                alert('Oops! Error reading JSON...\n\n' +
                    'URL: ' + url + '\n\n' +
                    'Error: ' + err.message);
                return;
            }
//            console.log("here is the JSON data...");
//            console.log(data);

            network.data = data;
            drawNetwork();
        });

        // while we wait for the data, set up the handlers...
        setUpClickHandler();
        setUpRadioButtonHandler();
        setUpKeyHandler();
        $(window).on('resize', resize);
    }

    function setUpClickHandler() {
        // click handler for "selectable" objects
        $(document).on('click', '.select-object', function () {
            // when any object of class "select-object" is clicked...
            var obj = network.lookup[$(this).data('id')];
            if (obj) {
                selectObject(obj);
            }
            // stop propagation of event (I think) ...
            return false;
        });
    }

    function setUpRadioButtonHandler() {
        d3.selectAll('#displayModes .radio').on('click', function () {
            var id = d3.select(this).attr('id');
            if (id !== viewMode) {
                radioButton('displayModes', id);
                viewMode = id;
                doRadioAction(id);
            }
        });
    }

    function doRadioAction(id) {
        showAllLayers();
        if (id === 'showPkt') {
            showPacketLayer();
        } else if (id === 'showOpt') {
            showOpticalLayer();
        }
    }

    function showAllLayers() {
        network.node.classed('inactive', false);
        network.link.classed('inactive', false);
    }

    function showPacketLayer() {
        network.node.each(function(d) {
            // deactivate nodes that are not hosts or switches
            if (d.class === 'device' && d.type !== 'switch') {
                d3.select(this).classed('inactive', true);
            }
        });

        network.link.each(function(lnk) {
            // deactivate infrastructure links that have opt's as endpoints
            if (lnk.source.type === 'roadm' || lnk.target.type === 'roadm') {
                d3.select(this).classed('inactive', true);
            }
        });
    }

    function showOpticalLayer() {
        network.node.each(function(d) {
            // deactivate nodes that are not optical devices
            if (d.type !== 'roadm') {
                d3.select(this).classed('inactive', true);
            }
        });

        network.link.each(function(lnk) {
            // deactivate infrastructure links that have opt's as endpoints
            if (lnk.source.type !== 'roadm' || lnk.target.type !== 'roadm') {
                d3.select(this).classed('inactive', true);
            }
        });
    }

    function setUpKeyHandler() {
        d3.select('body')
            .on('keydown', function () {
                processKeyEvent();
                if (debug('showKeyHandler')) {
                    network.svg.append('text')
                        .attr('x', 5)
                        .attr('y', 15)
                        .style('font-size', '20pt')
                        .text('keyCode: ' + d3.event.keyCode +
                            ' applied to : ' + contextLabel())
                        .transition().duration(2000)
                        .style('font-size', '2pt')
                        .style('fill-opacity', 0.01)
                        .remove();
                }
            });
    }

    function contextLabel() {
        return hovered === null ? "(nothing)" : hovered.id;
    }

    function radioButton(group, id) {
        d3.selectAll("#" + group + " .radio").classed("active", false);
        d3.select("#" + group + " #" + id).classed("active", true);
    }

    function processKeyEvent() {
        var code = d3.event.keyCode;
        switch (code) {
            case 71:    // G
                cycleLayout();
                break;
            case 76:    // L
                cycleLabels();
                break;
            case 80:    // P
                togglePorts();
                break;
            case 85:    // U
                unpin();
                break;
        }

    }

    function cycleLayout() {
        config.options.layering = !config.options.layering;
        network.force.resume();
    }

    function cycleLabels() {
        console.log('Cycle Labels - context = ' + contextLabel());
    }

    function togglePorts() {
        console.log('Toggle Ports - context = ' + contextLabel());
    }

    function unpin() {
        if (hovered) {
            hovered.fixed = false;
            findNodeFromData(hovered).classed('fixed', false);
            network.force.resume();
        }
        console.log('Unpin - context = ' + contextLabel());
    }


    // ========================================================

    function drawNetwork() {
        $('#view').empty();

        prepareNodesAndLinks();
        createLayout();
        console.log("\n\nHere is the augmented network object...");
        console.log(network);
    }

    function prepareNodesAndLinks() {
        network.lookup = {};
        network.nodes = [];
        network.links = [];

        var nw = network.forceWidth,
            nh = network.forceHeight;

        function yPosConstraintForNode(n) {
            return config.constraints.ypos[n.type || 'host'];
        }

        // Note that both 'devices' and 'hosts' get mapped into the nodes array

        // first, the devices...
        network.data.devices.forEach(function(n) {
            var ypc = yPosConstraintForNode(n),
                ix = Math.random() * 0.6 * nw + 0.2 * nw,
                iy = ypc * nh,
                node = {
                    id: n.id,
                    labels: n.labels,
                    class: 'device',
                    icon: 'device',
                    type: n.type,
                    x: ix,
                    y: iy,
                    constraint: {
                        weight: 0.7,
                        y: iy
                    }
                };
            network.lookup[n.id] = node;
            network.nodes.push(node);
        });

        // then, the hosts...
        network.data.hosts.forEach(function(n) {
            var ypc = yPosConstraintForNode(n),
                ix = Math.random() * 0.6 * nw + 0.2 * nw,
                iy = ypc * nh,
                node = {
                    id: n.id,
                    labels: n.labels,
                    class: 'host',
                    icon: 'host',
                    type: n.type,
                    x: ix,
                    y: iy,
                    constraint: {
                        weight: 0.7,
                        y: iy
                    }
                };
            network.lookup[n.id] = node;
            network.nodes.push(node);
        });


        // now, process the explicit links...
        network.data.links.forEach(function(lnk) {
            var src = network.lookup[lnk.src],
                dst = network.lookup[lnk.dst],
                id = src.id + "~" + dst.id;

            var link = {
                class: 'infra',
                id: id,
                type: lnk.type,
                width: lnk.linkWidth,
                source: src,
                target: dst,
                strength: config.force.linkStrength.infra
            };
            network.links.push(link);
        });

        // finally, infer host links...
        network.data.hosts.forEach(function(n) {
            var src = network.lookup[n.id],
                dst = network.lookup[n.cp.device],
                id = src.id + "~" + dst.id;

            var link = {
                class: 'host',
                id: id,
                type: 'hostLink',
                width: config.hostLinkWidth,
                source: src,
                target: dst,
                strength: config.force.linkStrength.host
            };
            network.links.push(link);
        });
    }

    function createLayout() {

        var cfg = config.force;

        network.force = d3.layout.force()
            .size([network.forceWidth, network.forceHeight])
            .nodes(network.nodes)
            .links(network.links)
            .linkStrength(function(d) { return cfg.linkStrength[d.class]; })
            .linkDistance(function(d) { return cfg.linkDistance[d.class]; })
            .charge(function(d) { return cfg.charge[d.class]; })
            .on('tick', tick);

        network.svg = d3.select('#view').append('svg')
            .attr('width', view.width)
            .attr('height', view.height)
            .append('g')
            .attr('transform', config.force.translate());
//            .attr('id', 'zoomable')
//            .call(d3.behavior.zoom().on("zoom", zoomRedraw));

//        function zoomRedraw() {
//            d3.select("#zoomable").attr("transform",
//                    "translate(" + d3.event.translate + ")"
//                    + " scale(" + d3.event.scale + ")");
//        }

        // TODO: move glow/blur stuff to util script
        var glow = network.svg.append('filter')
            .attr('x', '-50%')
            .attr('y', '-50%')
            .attr('width', '200%')
            .attr('height', '200%')
            .attr('id', 'blue-glow');

        glow.append('feColorMatrix')
            .attr('type', 'matrix')
            .attr('values', '0 0 0 0  0 ' +
                '0 0 0 0  0 ' +
                '0 0 0 0  .7 ' +
                '0 0 0 1  0 ');

        glow.append('feGaussianBlur')
            .attr('stdDeviation', 3)
            .attr('result', 'coloredBlur');

        glow.append('feMerge').selectAll('feMergeNode')
            .data(['coloredBlur', 'SourceGraphic'])
            .enter().append('feMergeNode')
            .attr('in', String);

        // TODO: legend (and auto adjust on scroll)
//        $('#view').on('scroll', function() {
//
//        });


        // add links to the display
        network.link = network.svg.append('g').selectAll('.link')
            .data(network.force.links(), function(d) {return d.id})
            .enter().append('line')
            .attr('class', function(d) {return 'link ' + d.class});


        // TODO: move drag behavior into separate method.
        // == define node drag behavior...
        network.draggedThreshold = d3.scale.linear()
            .domain([0, 0.1])
            .range([5, 20])
            .clamp(true);

        function dragged(d) {
            var threshold = network.draggedThreshold(network.force.alpha()),
                dx = d.oldX - d.px,
                dy = d.oldY - d.py;
            if (Math.abs(dx) >= threshold || Math.abs(dy) >= threshold) {
                d.dragged = true;
            }
            return d.dragged;
        }

        network.drag = d3.behavior.drag()
            .origin(function(d) { return d; })
            .on('dragstart', function(d) {
                d.oldX = d.x;
                d.oldY = d.y;
                d.dragged = false;
                d.fixed |= 2;
            })
            .on('drag', function(d) {
                d.px = d3.event.x;
                d.py = d3.event.y;
                if (dragged(d)) {
                    if (!network.force.alpha()) {
                        network.force.alpha(.025);
                    }
                }
            })
            .on('dragend', function(d) {
                if (!dragged(d)) {
                    selectObject(d, this);
                }
                d.fixed &= ~6;

                // once we've finished moving, pin the node in position,
                // if it is a device (not a host)
                if (d.class === 'device') {
                    d.fixed = true;
                    d3.select(this).classed('fixed', true)
                }
            });

        $('#view').on('click', function(e) {
            if (!$(e.target).closest('.node').length) {
                deselectObject();
            }
        });


        // add nodes to the display
        network.node = network.svg.selectAll('.node')
            .data(network.force.nodes(), function(d) {return d.id})
            .enter().append('g')
            .attr('class', function(d) {
                var cls = 'node ' + d.class;
                if (d.type) {
                    cls += ' ' + d.type;
                }
                return cls;
            })
            .attr('transform', function(d) {
                return translate(d.x, d.y);
            })
            .call(network.drag)
            .on('mouseover', function(d) {
                // TODO: show tooltip
                if (network.mouseoutTimeout) {
                    clearTimeout(network.mouseoutTimeout);
                    network.mouseoutTimeout = null;
                }
                hoverObject(d);
            })
            .on('mouseout', function(d) {
                // TODO: hide tooltip
                if (network.mouseoutTimeout) {
                    clearTimeout(network.mouseoutTimeout);
                    network.mouseoutTimeout = null;
                }
                network.mouseoutTimeout = setTimeout(function() {
                    hoverObject(null);
                }, config.mouseOutTimerDelayMs);
            });


        // deal with device nodes first
        network.nodeRect = network.node.filter('.device')
            .append('rect')
            .attr({
                rx: 5,
                ry: 5,
                width: 100,
                height: 12
            });
            // note that width/height are adjusted to fit the label text
            // then padded, and space made for the icon.

        network.node.filter('.device').each(function(d) {
            var node = d3.select(this),
                icon = iconUrl(d);

            node.append('text')
            // TODO: add label cycle behavior
                .text(d.id)
                .attr('dy', '1.1em');

            if (icon) {
                var cfg = config.icons;
                node.append('svg:image')
                    .attr({
                        width: cfg.w,
                        height: cfg.h,
                        'xlink:href': icon
                    });
                // note, icon relative positioning (x,y) is done after we have
                // adjusted the bounds of the rectangle...
            }

            // debug function to show the modelled x,y coordinates of nodes...
            if (debug('showNodeXY')) {
                node.select('rect').attr('fill-opacity', 0.5);
                node.append('circle')
                    .attr({
                        class: 'debug',
                        cx: 0,
                        cy: 0,
                        r: '3px'
                    });
            }
        });

        // now process host nodes
        network.nodeCircle = network.node.filter('.host')
            .append('circle')
            .attr({
                r: config.hostRadius
            });

        network.node.filter('.host').each(function(d) {
            var node = d3.select(this),
                icon = iconUrl(d);

            // debug function to show the modelled x,y coordinates of nodes...
            if (debug('showNodeXY')) {
                node.select('circle').attr('fill-opacity', 0.5);
                node.append('circle')
                    .attr({
                        class: 'debug',
                        cx: 0,
                        cy: 0,
                        r: '3px'
                    });
            }
        });

        // this function is scheduled to happen soon after the given thread ends
        setTimeout(function() {
            // post process the device nodes, to pad their size to fit the
            // label text and attach the icon to the right location.
            network.node.filter('.device').each(function(d) {
                // for every node, recompute size, padding, etc. so text fits
                var node = d3.select(this),
                    text = node.select('text'),
                    box = adjustRectToFitText(node),
                    lab = config.labels;

                // now make the computed adjustment
                node.select('rect')
                    .attr(box);

                node.select('image')
                    .attr('x', box.x + config.icons.xoff)
                    .attr('y', box.y + config.icons.yoff);

                var bounds = boundsFromBox(box);

                // todo: clean up extent and edge work..
                d.extent = {
                    left: bounds.x1 - lab.marginLR,
                    right: bounds.x2 + lab.marginLR,
                    top: bounds.y1 - lab.marginTB,
                    bottom: bounds.y2 + lab.marginTB
                };

                d.edge = {
                    left   : new geo.LineSegment(bounds.x1, bounds.y1, bounds.x1, bounds.y2),
                    right  : new geo.LineSegment(bounds.x2, bounds.y1, bounds.x2, bounds.y2),
                    top    : new geo.LineSegment(bounds.x1, bounds.y1, bounds.x2, bounds.y1),
                    bottom : new geo.LineSegment(bounds.x1, bounds.y2, bounds.x2, bounds.y2)
                };

            });

            network.numTicks = 0;
            network.preventCollisions = false;
            network.force.start();
            for (var i = 0; i < config.force.ticksWithoutCollisions; i++) {
                network.force.tick();
            }
            network.preventCollisions = true;
            $('#view').css('visibility', 'visible');
        });


        // returns the newly computed bounding box of the rectangle
        function adjustRectToFitText(n) {
            var text = n.select('text'),
                box = text.node().getBBox(),
                lab = config.labels;

            // not sure why n.data() returns an array of 1 element...
            var data = n.data()[0];

            text.attr('text-anchor', 'middle')
                .attr('y', '-0.8em')
                .attr('x', lab.imgPad/2)
            ;

            // translate the bbox so that it is centered on [x,y]
            box.x = -box.width / 2;
            box.y = -box.height / 2;

            // add padding
            box.x -= (lab.padLR + lab.imgPad/2);
            box.width += lab.padLR * 2 + lab.imgPad;
            box.y -= lab.padTB;
            box.height += lab.padTB * 2;

            return box;
        }

        function boundsFromBox(box) {
            return {
                x1: box.x,
                y1: box.y,
                x2: box.x + box.width,
                y2: box.y + box.height
            };
        }

    }

    function iconUrl(d) {
        return 'img/' + d.type + '.png';
//        return config.iconUrl[d.icon];
    }

    function translate(x, y) {
        return 'translate(' + x + ',' + y + ')';
    }

    // prevents collisions amongst device nodes
    function preventCollisions() {
        var quadtree = d3.geom.quadtree(network.nodes),
            hrad = config.hostRadius;

        network.nodes.forEach(function(n) {
            var nx1, nx2, ny1, ny2;

            if (n.class === 'device') {
                nx1 = n.x + n.extent.left;
                nx2 = n.x + n.extent.right;
                ny1 = n.y + n.extent.top;
                ny2 = n.y + n.extent.bottom;

            } else {
                nx1 = n.x - hrad;
                nx2 = n.x + hrad;
                ny1 = n.y - hrad;
                ny2 = n.y + hrad;
            }

            quadtree.visit(function(quad, x1, y1, x2, y2) {
                if (quad.point && quad.point !== n) {
                    // check if the rectangles/circles intersect
                    var p = quad.point,
                        px1, px2, py1, py2, ix;

                    if (p.class === 'device') {
                        px1 = p.x + p.extent.left;
                        px2 = p.x + p.extent.right;
                        py1 = p.y + p.extent.top;
                        py2 = p.y + p.extent.bottom;

                    } else {
                        px1 = p.x - hrad;
                        px2 = p.x + hrad;
                        py1 = p.y - hrad;
                        py2 = p.y + hrad;
                    }

                    ix = (px1 <= nx2 && nx1 <= px2 && py1 <= ny2 && ny1 <= py2);

                    if (ix) {
                        var xa1 = nx2 - px1, // shift n left , p right
                            xa2 = px2 - nx1, // shift n right, p left
                            ya1 = ny2 - py1, // shift n up   , p down
                            ya2 = py2 - ny1, // shift n down , p up
                            adj = Math.min(xa1, xa2, ya1, ya2);

                        if (adj == xa1) {
                            n.x -= adj / 2;
                            p.x += adj / 2;
                        } else if (adj == xa2) {
                            n.x += adj / 2;
                            p.x -= adj / 2;
                        } else if (adj == ya1) {
                            n.y -= adj / 2;
                            p.y += adj / 2;
                        } else if (adj == ya2) {
                            n.y += adj / 2;
                            p.y -= adj / 2;
                        }
                    }
                    return ix;
                }
            });

        });
    }

    function tick(e) {
        network.numTicks++;

        if (config.options.layering) {
            // adjust the y-coord of each node, based on y-pos constraints
            network.nodes.forEach(function (n) {
                var z = e.alpha * n.constraint.weight;
                if (!isNaN(n.constraint.y)) {
                    n.y = (n.constraint.y * z + n.y * (1 - z));
                }
            });
        }

        if (config.options.collisionPrevention && network.preventCollisions) {
            preventCollisions();
        }

        // clip visualization of links at bounds of nodes...
        network.link.each(function(d) {
                var xs = d.source.x,
                    ys = d.source.y,
                    xt = d.target.x,
                    yt = d.target.y,
                    line = new geo.LineSegment(xs, ys, xt, yt),
                    e, ix;

                for (e in d.source.edge) {
                    ix = line.intersect(d.source.edge[e].offset(xs, ys));
                    if (ix.in1 && ix.in2) {
                        xs = ix.x;
                        ys = ix.y;
                        break;
                    }
                }

                for (e in d.target.edge) {
                    ix = line.intersect(d.target.edge[e].offset(xt, yt));
                    if (ix.in1 && ix.in2) {
                        xt = ix.x;
                        yt = ix.y;
                        break;
                    }
                }

                d3.select(this)
                    .attr('x1', xs)
                    .attr('y1', ys)
                    .attr('x2', xt)
                    .attr('y2', yt);
            });

        // position each node by translating the node (group) by x,y
        network.node
            .attr('transform', function(d) {
                return translate(d.x, d.y);
            });

    }

    //    $('#docs-close').on('click', function() {
    //        deselectObject();
    //        return false;
    //    });

    //    $(document).on('click', '.select-object', function() {
    //        var obj = graph.data[$(this).data('name')];
    //        if (obj) {
    //            selectObject(obj);
    //        }
    //        return false;
    //    });

    function findNodeFromData(d) {
        var el = null;
        network.node.filter('.' + d.class).each(function(n) {
            if (n.id === d.id) {
                el = d3.select(this);
            }
        });
        return el;
    }

    function selectObject(obj, el) {
        var node;
        if (el) {
            node = d3.select(el);
        } else {
            network.node.each(function(d) {
                if (d == obj) {
                    node = d3.select(el = this);
                }
            });
        }
        if (!node) return;

        if (node.classed('selected')) {
            deselectObject();
            flyinPane(null);
            return;
        }
        deselectObject(false);

        selected = {
            obj : obj,
            el  : el
        };

        node.classed('selected', true);
        flyinPane(obj);
    }

    function deselectObject(doResize) {
        // Review: logic of 'resize(...)' function.
        if (doResize || typeof doResize == 'undefined') {
            resize(false);
        }

        // deselect all nodes in the network...
        network.node.classed('selected', false);
        selected = {};
        flyinPane(null);
    }

    function flyinPane(obj) {
        var pane = d3.select('#flyout'),
            url;

        if (obj) {
            // go get details of the selected object from the server...
            url = detailJsonUrl(obj.id);
            d3.json(url, function (err, data) {
                if (err) {
                    alert('Oops! Error reading JSON...\n\n' +
                        'URL: ' + url + '\n\n' +
                        'Error: ' + err.message);
                    return;
                }
//                console.log("JSON data... " + url);
//                console.log(data);

                displayDetails(data, pane);
            });

        } else {
            // hide pane
            pane.transition().duration(750)
                .style('right', '-320px')
                .style('opacity', 0.0);
        }
    }

    function displayDetails(data, pane) {
        $('#flyout').empty();

        var title = pane.append("h2"),
            table = pane.append("table"),
            tbody = table.append("tbody");

        $('<img src="img/' + data.type + '.png">').appendTo(title);
        $('<span>').attr('class', 'icon').text(data.id).appendTo(title);


        // TODO: consider using d3 data bind to TR/TD

        data.propOrder.forEach(function(p) {
            if (p === '-') {
                addSep(tbody);
            } else {
                addProp(tbody, p, data.props[p]);
            }
        });

        function addSep(tbody) {
            var tr = tbody.append('tr');
            $('<hr>').appendTo(tr.append('td').attr('colspan', 2));
        }

        function addProp(tbody, label, value) {
            var tr = tbody.append('tr');

            tr.append('td')
                .attr('class', 'label')
                .text(label + ' :');

            tr.append('td')
                .attr('class', 'value')
                .text(value);
        }

        // show pane
        pane.transition().duration(750)
            .style('right', '20px')
            .style('opacity', 1.0);
    }

    function highlightObject(obj) {
        if (obj) {
            if (obj != highlighted) {
                // TODO set or clear "inactive" class on nodes, based on criteria
                network.node.classed('inactive', function(d) {
                    //                return (obj !== d &&
                    //                    d.relation(obj.id));
                    return (obj !== d);
                });
                // TODO: same with links
                network.link.classed('inactive', function(d) {
                    return (obj !== d.source && obj !== d.target);
                });
            }
            highlighted = obj;
        } else {
            if (highlighted) {
                // clear the inactive flag (no longer suppressed visually)
                network.node.classed('inactive', false);
                network.link.classed('inactive', false);
            }
            highlighted = null;

        }
    }

    function hoverObject(obj) {
        if (obj) {
            hovered = obj;
        } else {
            if (hovered) {
                hovered = null;
            }
        }
    }


    function resize() {
        view.height = window.innerHeight - config.mastHeight;
        view.width = window.innerWidth;
        $('#view')
            .css('height', view.height + 'px')
            .css('width', view.width + 'px');

        network.forceWidth = view.width - config.force.marginLR;
        network.forceHeight = view.height - config.force.marginTB;
    }

    // ======================================================================
    // register with the UI framework

    api.addView('network', {
        load: loadNetworkView
    });


}(ONOS));

