/*
 ONOS network topology viewer - PoC version 1.0

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    var api = onos.api;

    var config = {
            jsonUrl: 'network.json',
            mastHeight: 32,
            force: {
                linkDistance: 150,
                linkStrength: 0.9,
                charge: -400,
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
                padLR: 3,
                padTB: 2,
                marginLR: 3,
                marginTB: 2
            },
            constraints: {
                ypos: {
                    pkt: 0.3,
                    opt: 0.7
                }
            }
        },
        view = {},
        network = {},
        selected = {},
        highlighted = null;


    function loadNetworkView() {
        // Hey, here I am, calling something on the ONOS api:
        api.printTime();

        resize();

        d3.json(config.jsonUrl, function (err, data) {
            if (err) {
                alert('Oops! Error reading JSON...\n\n' +
                    'URL: ' + jsonUrl + '\n\n' +
                    'Error: ' + err.message);
                return;
            }
            console.log("here is the JSON data...");
            console.log(data);

            network.data = data;
            drawNetwork();
        });

        $(document).on('click', '.select-object', function() {
            // when any object of class "select-object" is clicked...
            // TODO: get a reference to the object via lookup...
            var obj = network.lookup[$(this).data('id')];
            if (obj) {
                selectObject(obj);
            }
            // stop propagation of event (I think) ...
            return false;
        });

        $(window).on('resize', resize);
    }


    // ========================================================

    function drawNetwork() {
        $('#view').empty();

        prepareNodesAndLinks();
        createLayout();
        console.log("\n\nHere is the augmented network object...");
        console.warn(network);
    }

    function prepareNodesAndLinks() {
        network.lookup = {};
        network.nodes = [];
        network.links = [];

        var nw = network.forceWidth,
            nh = network.forceHeight;

        network.data.nodes.forEach(function(n) {
            var ypc = yPosConstraintForNode(n),
                ix = Math.random() * 0.8 * nw + 0.1 * nw,
                iy = ypc * nh,
                node = {
                    id: n.id,
                    type: n.type,
                    status: n.status,
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

        function yPosConstraintForNode(n) {
            return config.constraints.ypos[n.type] || 0.5;
        }


        network.data.links.forEach(function(n) {
            var src = network.lookup[n.src],
                dst = network.lookup[n.dst],
                id = src.id + "~" + dst.id;

            var link = {
                id: id,
                source: src,
                target: dst,
                strength: config.force.linkStrength
            };
            network.links.push(link);
        });
    }

    function createLayout() {

        network.force = d3.layout.force()
            .nodes(network.nodes)
            .links(network.links)
            .linkStrength(function(d) { return d.strength; })
            .size([network.forceWidth, network.forceHeight])
            .linkDistance(config.force.linkDistance)
            .charge(config.force.charge)
            .on('tick', tick);

        network.svg = d3.select('#view').append('svg')
            .attr('width', view.width)
            .attr('height', view.height)
            .append('g')
            .attr('transform', config.force.translate());

        // TODO: svg.append('defs')
        // TODO: glow/blur stuff
        // TODO: legend (and auto adjust on scroll)

        network.link = network.svg.append('g').selectAll('.link')
            .data(network.force.links(), function(d) {return d.id})
            .enter().append('line')
            .attr('class', 'link');

        // TODO: drag behavior
        // TODO: closest node deselect

        // TODO: add drag, mouseover, mouseout behaviors
        network.node = network.svg.selectAll('.node')
            .data(network.force.nodes(), function(d) {return d.id})
            .enter().append('g')
            .attr('class', 'node')
            .attr('transform', function(d) {
                return translate(d.x, d.y);
            })
        //        .call(network.drag)
            .on('mouseover', function(d) {})
            .on('mouseout', function(d) {});

        // TODO: augment stroke and fill functions
        network.nodeRect = network.node.append('rect')
            // TODO: css for node rects
            .attr('rx', 5)
            .attr('ry', 5)
            .attr('stroke', function(d) { return '#000'})
            .attr('fill', function(d) { return '#ddf'})
            .attr('width', 60)
            .attr('height', 24);

        network.node.each(function(d) {
            var node = d3.select(this),
                rect = node.select('rect');
            var text = node.append('text')
                .text(d.id)
                .attr('dx', '1em')
                .attr('dy', '2.1em');
        });

        // this function is scheduled to happen soon after the given thread ends
        setTimeout(function() {
            network.node.each(function(d) {
                // for every node, recompute size, padding, etc. so text fits
                var node = d3.select(this),
                    text = node.selectAll('text'),
                    bounds = {},
                    first = true;

                // NOTE: probably unnecessary code if we only have one line.
            });

            network.numTicks = 0;
            network.preventCollisions = false;
            network.force.start();
            for (var i = 0; i < config.ticksWithoutCollisions; i++) {
                network.force.tick();
            }
            network.preventCollisions = true;
            $('#view').css('visibility', 'visible');
        });

    }

    function translate(x, y) {
        return 'translate(' + x + ',' + y + ')';
    }


    function tick(e) {
        network.numTicks++;

        // adjust the y-coord of each node, based on y-pos constraints
//        network.nodes.forEach(function (n) {
//            var z = e.alpha * n.constraint.weight;
//            if (!isNaN(n.constraint.y)) {
//                n.y = (n.constraint.y * z + n.y * (1 - z));
//            }
//        });

        network.link
            .attr('x1', function(d) {
                return d.source.x;
            })
            .attr('y1', function(d) {
                return d.source.y;
            })
            .attr('x2', function(d) {
                return d.target.x;
            })
            .attr('y2', function(d) {
                return d.target.y;
            });

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
            return;
        }
        deselectObject(false);

        selected = {
            obj : obj,
            el  : el
        };

        highlightObject(obj);

        node.classed('selected', true);

        // TODO animate incoming info pane
        // resize(true);
        // TODO: check bounds of selected node and scroll into view if needed
    }

    function deselectObject(doResize) {
        // Review: logic of 'resize(...)' function.
        if (doResize || typeof doResize == 'undefined') {
            resize(false);
        }
        // deselect all nodes in the network...
        network.node.classed('selected', false);
        selected = {};
        highlightObject(null);
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

    function resize(showDetails) {
        console.log("resize() called...");

        var $details = $('#details');

        if (typeof showDetails == 'boolean') {
            var showingDetails = showDetails;
            // TODO: invoke $details.show() or $details.hide()...
            //        $details[showingDetails ? 'show' : 'hide']();
        }

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

