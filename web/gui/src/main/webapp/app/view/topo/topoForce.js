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
 ONOS GUI -- Topology Event Module.
 Defines event handling for events received from the server.
 */

(function () {
    'use strict';

    // injected refs
    var $log, sus, is, ts, tis, xlink;

    // configuration
    var labelConfig = {
            imgPad: 16,
            padLR: 4,
            padTB: 3,
            marginLR: 3,
            marginTB: 2,
            port: {
                gap: 3,
                width: 18,
                height: 14
            }
        };

    var deviceIconConfig = {
         xoff: -20,
         yoff: -18
    };

    // internal state
    var settings,   // merged default settings and options
        force,      // force layout object
        drag,       // drag behavior handler
        network = {
            nodes: [],
            links: [],
            lookup: {},
            revLinkToKey: {}
        },
        projection,             // background map projection
        deviceLabelIndex = 0,   // for device label cycling
        hostLabelIndex = 0;     // for host label cycling

    // SVG elements;
    var linkG, linkLabelG, nodeG;

    // D3 selections;
    var link, linkLabel, node;

    // default settings for force layout
    var defaultSettings = {
        gravity: 0.4,
        friction: 0.7,
        charge: {
            // note: key is node.class
            device: -8000,
            host: -5000,
            _def_: -12000
        },
        linkDistance: {
            // note: key is link.type
            direct: 100,
            optical: 120,
            hostLink: 3,
            _def_: 50
        },
        linkStrength: {
            // note: key is link.type
            // range: {0.0 ... 1.0}
            //direct: 1.0,
            //optical: 1.0,
            //hostLink: 1.0,
            _def_: 1.0
        }
    };


    // ==========================
    // === EVENT HANDLERS

    function addDevice(data) {
        var id = data.id,
            d;

        xlink.showNoDevs(false);

        // although this is an add device event, if we already have the
        //  device, treat it as an update instead..
        if (network.lookup[id]) {
            updateDevice(data);
            return;
        }

        d = createDeviceNode(data);
        network.nodes.push(d);
        network.lookup[id] = d;

        $log.debug("Created new device.. ", d.id, d.x, d.y);

        updateNodes();
        fStart();
    }

    function updateDevice(data) {
        var id = data.id,
            d = network.lookup[id],
            wasOnline;

        if (d) {
            wasOnline = d.online;
            angular.extend(d, data);
            if (positionNode(d, true)) {
                sendUpdateMeta(d, true);
            }
            updateNodes();
            if (wasOnline !== d.online) {
                // TODO: re-instate link update, and offline visibility
                //findAttachedLinks(d.id).forEach(restyleLinkElement);
                //updateOfflineVisibility(d);
            }
        } else {
            // TODO: decide whether we want to capture logic errors
            //logicError('updateDevice lookup fail. ID = "' + id + '"');
        }
    }

    function sendUpdateMeta(d, store) {
        var metaUi = {},
            ll;

        // TODO: fix this code to send event to server...
        //if (store) {
        //    ll = geoMapProj.invert([d.x, d.y]);
        //    metaUi = {
        //        x: d.x,
        //        y: d.y,
        //        lng: ll[0],
        //        lat: ll[1]
        //    };
        //}
        //d.metaUi = metaUi;
        //sendMessage('updateMeta', {
        //    id: d.id,
        //    'class': d.class,
        //    memento: metaUi
        //});
    }


    function fStart() {
        $log.debug('TODO fStart()...');
        // TODO...
    }

    function fResume() {
        $log.debug('TODO fResume()...');
        // TODO...
    }

    // ==========================
    // === Devices and hosts - helper functions

    function coordFromLngLat(loc) {
        // Our hope is that the projection is installed before we start
        // handling incoming nodes. But if not, we'll just return the origin.
        return projection ? projection([loc.lng, loc.lat]) : [0, 0];
    }

    function positionNode(node, forUpdate) {
        var meta = node.metaUi,
            x = meta && meta.x,
            y = meta && meta.y,
            xy;

        // If we have [x,y] already, use that...
        if (x && y) {
            node.fixed = true;
            node.px = node.x = x;
            node.py = node.y = y;
            return;
        }

        var location = node.location,
            coord;

        if (location && location.type === 'latlng') {
            coord = coordFromLngLat(location);
            node.fixed = true;
            node.px = node.x = coord[0];
            node.py = node.y = coord[1];
            return true;
        }

        // if this is a node update (not a node add).. skip randomizer
        if (forUpdate) {
            return;
        }

        // Note: Placing incoming unpinned nodes at exactly the same point
        //        (center of the view) causes them to explode outwards when
        //        the force layout kicks in. So, we spread them out a bit
        //        initially, to provide a more serene layout convergence.
        //       Additionally, if the node is a host, we place it near
        //        the device it is connected to.

        function spread(s) {
            return Math.floor((Math.random() * s) - s/2);
        }

        function randDim(dim) {
            return dim / 2 + spread(dim * 0.7071);
        }

        function rand() {
            return {
                x: randDim(network.view.width()),
                y: randDim(network.view.height())
            };
        }

        function near(node) {
            var min = 12,
                dx = spread(12),
                dy = spread(12);
            return {
                x: node.x + min + dx,
                y: node.y + min + dy
            };
        }

        function getDevice(cp) {
            var d = network.lookup[cp.device];
            return d || rand();
        }

        xy = (node.class === 'host') ? near(getDevice(node.cp)) : rand();
        angular.extend(node, xy);
    }

    function createDeviceNode(device) {
        // start with the object as is
        var node = device,
            type = device.type,
            svgCls = type ? 'node device ' + type : 'node device';

        // Augment as needed...
        node.class = 'device';
        node.svgClass = device.online ? svgCls + ' online' : svgCls;
        positionNode(node);
        return node;
    }

    // ==========================
    // === Devices and hosts - D3 rendering

    // Returns the newly computed bounding box of the rectangle
    function adjustRectToFitText(n) {
        var text = n.select('text'),
            box = text.node().getBBox(),
            lab = labelConfig;

        text.attr('text-anchor', 'middle')
            .attr('y', '-0.8em')
            .attr('x', lab.imgPad/2);

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

    function mkSvgClass(d) {
        return d.fixed ? d.svgClass + ' fixed' : d.svgClass;
    }

    function hostLabel(d) {
        var idx = (hostLabelIndex < d.labels.length) ? hostLabelIndex : 0;
        return d.labels[idx];
    }
    function deviceLabel(d) {
        var idx = (deviceLabelIndex < d.labels.length) ? deviceLabelIndex : 0;
        return d.labels[idx];
    }
    function trimLabel(label) {
        return (label && label.trim()) || '';
    }

    function emptyBox() {
        return {
            x: -2,
            y: -2,
            width: 4,
            height: 4
        };
    }


    function updateDeviceLabel(d) {
        var label = trimLabel(deviceLabel(d)),
            noLabel = !label,
            node = d.el,
            dim = is.iconConfig().device.dim,
            devCfg = deviceIconConfig,
            box, dx, dy;

        node.select('text')
            .text(label)
            .style('opacity', 0)
            .transition()
            .style('opacity', 1);

        if (noLabel) {
            box = emptyBox();
            dx = -dim/2;
            dy = -dim/2;
        } else {
            box = adjustRectToFitText(node);
            dx = box.x + devCfg.xoff;
            dy = box.y + devCfg.yoff;
        }

        node.select('rect')
            .transition()
            .attr(box);

        node.select('g.deviceIcon')
            .transition()
            .attr('transform', sus.translate(dx, dy));
    }

    function updateHostLabel(d) {
        var label = trimLabel(hostLabel(d));
        d.el.select('text').text(label);
    }

    function nodeMouseOver(m) {
        // TODO
        $log.debug("TODO nodeMouseOver()...", m);
    }

    function nodeMouseOut(m) {
        // TODO
        $log.debug("TODO nodeMouseOut()...", m);
    }

    function updateDeviceColors(d) {
        if (d) {
            setDeviceColor(d);
        } else {
            node.filter('.device').each(function (d) {
                setDeviceColor(d);
            });
        }
    }

    var dCol = {
        black: '#000',
        paleblue: '#acf',
        offwhite: '#ddd',
        darkgrey: '#444',
        midgrey: '#888',
        lightgrey: '#bbb',
        orange: '#f90'
    };

    // note: these are the device icon colors without affinity
    var dColTheme = {
        light: {
            rfill: dCol.offwhite,
            online: {
                glyph: dCol.darkgrey,
                rect: dCol.paleblue
            },
            offline: {
                glyph: dCol.midgrey,
                rect: dCol.lightgrey
            }
        },
        dark: {
            rfill: dCol.midgrey,
            online: {
                glyph: dCol.darkgrey,
                rect: dCol.paleblue
            },
            offline: {
                glyph: dCol.midgrey,
                rect: dCol.darkgrey
            }
        }
    };

    function devBaseColor(d) {
        var o = d.online ? 'online' : 'offline';
        return dColTheme[ts.theme()][o];
    }

    function setDeviceColor(d) {
        var o = d.online,
            s = d.el.classed('selected'),
            c = devBaseColor(d),
            a = instColor(d.master, o),
            icon = d.el.select('g.deviceIcon'),
            g, r;

        if (s) {
            g = c.glyph;
            r = dCol.orange;
        } else if (tis.isVisible()) {
            g = o ? a : c.glyph;
            r = o ? c.rfill : a;
        } else {
            g = c.glyph;
            r = c.rect;
        }

        icon.select('use').style('fill', g);
        icon.select('rect').style('fill', r);
    }

    function instColor(id, online) {
        return sus.cat7().getColor(id, !online, ts.theme());
    }

    //============

    function updateNodes() {
        node = nodeG.selectAll('.node')
            .data(network.nodes, function (d) { return d.id; });

        // operate on existing nodes...
        node.filter('.device').each(deviceExisting);
        node.filter('.host').each(hostExisting);

        // operate on entering nodes:
        var entering = node.enter()
            .append('g')
            .attr({
                id: function (d) { return sus.safeId(d.id); },
                class: mkSvgClass,
                transform: function (d) { return sus.translate(d.x, d.y); },
                opacity: 0
            })
            .call(drag)
            .on('mouseover', nodeMouseOver)
            .on('mouseout', nodeMouseOut)
            .transition()
            .attr('opacity', 1);

        // augment nodes...
        entering.filter('.device').each(deviceEnter);
        entering.filter('.host').each(hostEnter);

        // operate on both existing and new nodes:
        updateDeviceColors();

        // operate on exiting nodes:
        // Note that the node is removed after 2 seconds.
        // Sub element animations should be shorter than 2 seconds.
        var exiting = node.exit()
            .transition()
            .duration(2000)
            .style('opacity', 0)
            .remove();

        // node specific....
        exiting.filter('.host').each(hostExit);
        exiting.filter('.device').each(deviceExit);

        // finally, resume the force layout
        fResume();
    }

    // ==========================
    // updateNodes - subfunctions

    function deviceExisting(d) {
        var node = d.el;
        node.classed('online', d.online);
        updateDeviceLabel(d);
        positionNode(d, true);
    }

    function hostExisting(d) {
        updateHostLabel(d);
        positionNode(d, true);
    }

    function deviceEnter(d) {
        var node = d3.select(this),
            glyphId = d.type || 'unknown',
            label = trimLabel(deviceLabel(d)),
            devCfg = deviceIconConfig,
            noLabel = !label,
            box, dx, dy, icon;

        d.el = node;

        node.append('rect').attr({ rx: 5, ry: 5 });
        node.append('text').text(label).attr('dy', '1.1em');
        box = adjustRectToFitText(node);
        node.select('rect').attr(box);

        icon = is.addDeviceIcon(node, glyphId);

        if (noLabel) {
            dx = -icon.dim/2;
            dy = -icon.dim/2;
        } else {
            box = adjustRectToFitText(node);
            dx = box.x + devCfg.xoff;
            dy = box.y + devCfg.yoff;
        }

        icon.attr('transform', sus.translate(dx, dy));
    }

    function hostEnter(d) {
        var node = d3.select(this);

            //cfg = config.icons.host,
            //r = cfg.radius[d.type] || cfg.defaultRadius,
            //textDy = r + 10,
        //TODO:     iid = iconGlyphUrl(d),
        //    _dummy;

        d.el = node;

        //TODO: showHostVis(node);

        node.append('circle').attr('r', r);
        //if (iid) {
            //TODO: addHostIcon(node, r, iid);
        //}
        node.append('text')
            .text(hostLabel)
            //.attr('dy', textDy)
            .attr('text-anchor', 'middle');
    }

    function hostExit(d) {
        var node = d.el;
        node.select('use')
            .style('opacity', 0.5)
            .transition()
            .duration(800)
            .style('opacity', 0);

        node.select('text')
            .style('opacity', 0.5)
            .transition()
            .duration(800)
            .style('opacity', 0);

        node.select('circle')
            .style('stroke-fill', '#555')
            .style('fill', '#888')
            .style('opacity', 0.5)
            .transition()
            .duration(1500)
            .attr('r', 0);
    }

    function deviceExit(d) {
        var node = d.el;
        node.select('use')
            .style('opacity', 0.5)
            .transition()
            .duration(800)
            .style('opacity', 0);

        node.selectAll('rect')
            .style('stroke-fill', '#555')
            .style('fill', '#888')
            .style('opacity', 0.5);
    }


    // ==========================
    // force layout tick function
    function tick() {

    }


    // ==========================
    // === MOUSE GESTURE HANDLERS

    function selectCb() { }
    function atDragEnd() {}
    function dragEnabled() {}
    function clickEnabled() {}


    // ==========================
    // Module definition

    angular.module('ovTopo')
    .factory('TopoForceService',
        ['$log', 'SvgUtilService', 'IconService', 'ThemeService',
            'TopoInstService',

        function (_$log_, _sus_, _is_, _ts_, _tis_) {
            $log = _$log_;
            sus = _sus_;
            is = _is_;
            ts = _ts_;
            tis = _tis_;

            // forceG is the SVG group to display the force layout in
            // xlink is the cross-link api from the main topo source file
            // w, h are the initial dimensions of the SVG
            // opts are, well, optional :)
            function initForce(forceG, _xlink_, w, h, opts) {
                $log.debug('initForce().. WxH = ' + w + 'x' + h);
                xlink = _xlink_;

                settings = angular.extend({}, defaultSettings, opts);

                // when the projection promise is resolved, cache the projection
                xlink.projectionPromise.then(
                    function (proj) {
                        projection = proj;
                        $log.debug('** We installed the projection: ', proj);
                    }
                );

                linkG = forceG.append('g').attr('id', 'topo-links');
                linkLabelG = forceG.append('g').attr('id', 'topo-linkLabels');
                nodeG = forceG.append('g').attr('id', 'topo-nodes');

                link = linkG.selectAll('.link');
                linkLabel = linkLabelG.selectAll('.linkLabel');
                node = nodeG.selectAll('.node');

                force = d3.layout.force()
                    .size([w, h])
                    .nodes(network.nodes)
                    .links(network.links)
                    .gravity(settings.gravity)
                    .friction(settings.friction)
                    .charge(settings.charge._def_)
                    .linkDistance(settings.linkDistance._def_)
                    .linkStrength(settings.linkStrength._def_)
                    .on('tick', tick);

                drag = sus.createDragBehavior(force,
                    selectCb, atDragEnd, dragEnabled, clickEnabled);
            }

            function resize(dim) {
                force.size([dim.width, dim.height]);
                // Review -- do we need to nudge the layout ?
            }

            return {
                initForce: initForce,
                resize: resize,

                updateDeviceColors: updateDeviceColors,

                addDevice: addDevice,
                updateDevice: updateDevice
            };
        }]);
}());
