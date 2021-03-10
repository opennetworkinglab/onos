/*
 * Copyright 2015-present Open Networking Foundation
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
 ONOS GUI -- Topology Model Module.
 Auxiliary functions for the model of the topology; that is, our internal
  representations of devices, hosts, links, etc.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, rnd;

    // api to topoForce
    var api;
    /*
       projection()
       network {...}
       restyleLinkElement( ldata )
       removeLinkElement( ldata )
     */

    // shorthand
    var lu, rlk, nodes, links, linksByDevice;

    var dim; // dimensions of layout [w,h]

    // configuration 'constants'
    var defaultLinkType = 'direct',
        nearDist = 15;


    function coordFromLngLat(loc) {
        var p = api.projection();
        return p ? p([loc.longOrX, loc.latOrY]) : [0, 0];
    }

    function lngLatFromCoord(coord) {
        var p = api.projection();
        return p ? p.invert(coord) : [0, 0];
    }

    function coordFromXY(loc) {
        var bgWidth = 1000,
            bgHeight = 1000;

        var scale = 1000 / bgWidth,
            yOffset = (1000 - (bgHeight * scale)) / 2;

        // 1000 is a hardcoded HTML value of the SVG element (topo2.html)
        var x = scale * loc.longOrX,
            y = (scale * loc.latOrY) + yOffset;

        return [x, y];
    }

    function positionNode(node, forUpdate) {
        var meta = node.metaUi,
            x = meta && meta.x,
            y = meta && meta.y,
            xy;

        // if the device contains explicit LONG/LAT data, use that to position
        if (setLongLat(node)) {
            // indicate we want to update cached meta data...
            return false;
        }

        // else if we have [x,y] cached in meta data, use that...
        if (x != undefined && y != undefined) {
            node.fixed = true;
            node.px = node.x = x;
            node.py = node.y = y;
            return true;
        }

        // if this is a node update (not a node add).. skip randomizer
        if (forUpdate && node.x != undefined && node.y != undefined) {
            return false;
        }

        // Note: Placing incoming unpinned nodes at exactly the same point
        //        (center of the view) causes them to explode outwards when
        //        the force layout kicks in. So, we spread them out a bit
        //        initially, to provide a more serene layout convergence.
        //       Additionally, if the node is a host, we place it near
        //        the device it is connected to.

        function rand() {
            return {
                x: rnd.randDim(dim[0]),
                y: rnd.randDim(dim[1]),
            };
        }

        function near(node) {
            return {
                x: node.x + nearDist + rnd.spread(nearDist),
                y: node.y + nearDist + rnd.spread(nearDist),
            };
        }

        function getDevice(cp) {
            var d = lu[cp.device];
            return d || rand();
        }

        node.fixed = false;
        xy = (node.class === 'host') ? near(getDevice(node.cp)) : rand();
        angular.extend(node, xy);
        return false;
    }

    function setLongLat(node) {
        var loc = node.location,
            coord;

        if (!loc || loc.locType === 'none') {
            node.fixed = false;

        } else if (loc) {
            coord = loc.locType === 'geo' ? coordFromLngLat(loc) : coordFromXY(loc);
            node.fixed = true;
            node.px = node.x = coord[0];
            node.py = node.y = coord[1];
            return true;
        }
        return false;
    }

    function resetAllLocations() {
        nodes.forEach(function (d) {
            setLongLat(d);
        });
    }

    function mkSvgCls(dh, t, on) {
        var ndh = 'node ' + dh,
            ndht = t ? ndh + ' ' + t : ndh;
        return on ? ndht + ' online' : ndht;
    }

    function createDeviceNode(device) {
        var node = device;

        // Augment as needed...
        node.class = 'device';
        node.svgClass = mkSvgCls('device', device.type, device.online);
        positionNode(node);
        return node;
    }

    function createHostNode(host) {
        var node = host;

        // Augment as needed...
        node.class = 'host';
        if (!node.type) {
            node.type = 'endstation';
        }
        node.svgClass = mkSvgCls('host', node.type);
        positionNode(node);
        return node;
    }

    function createHostLink(hostId, devId, devPort, connectionType) {
        var linkKey = hostId + '/0-' + devId + '/' + devPort,
            lnk = linkEndPoints(hostId, devId);

        if (!lnk) {
            return null;
        }

        // Synthesize link ...
        angular.extend(lnk, {
            key: linkKey,
            class: 'link',
            // NOTE: srcPort left undefined (host end of the link)
            tgtPort: devPort,

            type: function () { return 'hostLink'; },
            connectionType: function () { return connectionType; },
            expected: function () { return true; },
            online: function () {
                // hostlink target is edge switch
                return lnk.target.online;
            },
            linkWidth: function () { return 1; },
        });
        return lnk;
    }

    function createLink(link) {
        var lnk = linkEndPoints(link.src, link.dst);

        if (!lnk) {
            return null;
        }

        angular.extend(lnk, {
            key: link.id,
            class: 'link',
            fromSource: link,
            srcPort: link.srcPort,
            tgtPort: link.dstPort,
            position: {
                x1: 0,
                y1: 0,
                x2: 0,
                y2: 0,
            },

            // functions to aggregate dual link state
            type: function () {
                var s = lnk.fromSource,
                    t = lnk.fromTarget;
                return (s && s.type) || (t && t.type) || defaultLinkType;
            },
            expected: function () {
                var s = lnk.fromSource,
                    t = lnk.fromTarget;
                return (s && s.expected) && (t && t.expected);
            },
            online: function () {
                var s = lnk.fromSource,
                    t = lnk.fromTarget,
                    both = lnk.source.online && lnk.target.online;
                return both && (s && s.online) && (t && t.online);
            },
            linkWidth: function () {
                var s = lnk.fromSource,
                    t = lnk.fromTarget,
                    ws = (s && s.linkWidth) || 0,
                    wt = (t && t.linkWidth) || 0;
                return lnk.position.multiLink ? 5 : Math.max(ws, wt);
            },
            extra: link.extra,
        });
        return lnk;
    }


    function linkEndPoints(srcId, dstId) {
        var srcNode = lu[srcId],
            dstNode = lu[dstId],
            sMiss = !srcNode ? missMsg('src', srcId) : '',
            dMiss = !dstNode ? missMsg('dst', dstId) : '';

        if (sMiss || dMiss) {
            $log.error('Node(s) not on map for link:' + sMiss + dMiss);
            // logicError('Node(s) not on map for link:\n' + sMiss + dMiss);
            return null;
        }

        return {
            source: srcNode,
            target: dstNode,
        };
    }

    function missMsg(what, id) {
        return '\n[' + what + '] "' + id + '" missing';
    }


    function makeNodeKey(d, what) {
        var port = what + 'Port';
        return d[what] + '/' + d[port];
    }

    function makeLinkKey(d, flipped) {
        var one = flipped ? makeNodeKey(d, 'dst') : makeNodeKey(d, 'src'),
            two = flipped ? makeNodeKey(d, 'src') : makeNodeKey(d, 'dst');
        return one + '-' + two;
    }

    function findLinkById(id) {
        // check to see if this is a reverse lookup, else default to given id
        var key = rlk[id] || id;
        return key && lu[key];
    }

    function findLink(linkData, op) {
        var key = makeLinkKey(linkData),
            keyrev = makeLinkKey(linkData, 1),
            link = lu[key],
            linkRev = lu[keyrev],
            result = {},
            ldata = link || linkRev,
            rawLink;

        if (op === 'add') {
            if (link) {
                // trying to add a link that we already know about
                result.ldata = link;
                result.badLogic = 'addLink: link already added';

            } else if (linkRev) {
                // we found the reverse of the link to be added
                result.ldata = linkRev;
                if (linkRev.fromTarget) {
                    result.badLogic = 'addLink: link already added';
                }
            }
        } else if (op === 'update') {
            if (!ldata) {
                result.badLogic = 'updateLink: link not found';
            } else {
                rawLink = link ? ldata.fromSource : ldata.fromTarget;
                result.updateWith = function (data) {
                    angular.extend(rawLink, data);
                    api.restyleLinkElement(ldata);
                };
            }
        } else if (op === 'remove') {
            if (!ldata) {
                result.badLogic = 'removeLink: link not found';
            } else {
                rawLink = link ? ldata.fromSource : ldata.fromTarget;

                if (!rawLink) {
                    result.badLogic = 'removeLink: link not found';

                } else {
                    result.removeRawLink = function () {
                        // remove link out of aggregate linksByDevice list
                        var linksForDevPair = linksByDevice[ldata.devicePair],
                            rmvIdx = fs.find(ldata.key, linksForDevPair, 'key');
                        if (rmvIdx >= 0) {
                            linksForDevPair.splice(rmvIdx, 1);
                        }
                        ldata.position.multilink = linksForDevPair.length >= 5;

                        if (link) {
                            // remove fromSource
                            ldata.fromSource = null;
                            if (ldata.fromTarget) {
                                // promote target into source position
                                ldata.fromSource = ldata.fromTarget;
                                ldata.fromTarget = null;
                                ldata.key = keyrev;
                                delete lu[key];
                                lu[keyrev] = ldata;
                                delete rlk[keyrev];
                            }
                        } else {
                            // remove fromTarget
                            ldata.fromTarget = null;
                            delete rlk[keyrev];
                        }
                        if (ldata.fromSource) {
                            api.restyleLinkElement(ldata);
                        } else {
                            api.removeLinkElement(ldata);
                        }
                    };
                }
            }
        }
        return result;
    }

    function findDevices(offlineOnly) {
        var a = [];
        nodes.forEach(function (d) {
            if (d.class === 'device' && !(offlineOnly && d.online)) {
                a.push(d);
            }
        });
        return a;
    }

    function findHosts() {
        var hosts = [];
        nodes.forEach(function (d) {
            if (d.class === 'host') {
                hosts.push(d);
            }
        });
        return hosts;
    }

    function findAttachedHosts(devId) {
        var hosts = [];
        nodes.forEach(function (d) {
            if (d.class === 'host' && d.cp.device === devId) {
                hosts.push(d);
            }
        });
        return hosts;
    }

    function findAttachedLinks(devId) {
        var lnks = [];
        links.forEach(function (d) {
            if (d.source.id === devId || d.target.id === devId) {
                lnks.push(d);
            }
        });
        return lnks;
    }

    // returns one-way links or where the internal link types differ
    function findBadLinks() {
        var lnks = [],
            src, tgt;
        links.forEach(function (d) {
            // NOTE: skip edge links, which are synthesized
            if (d.type() !== 'hostLink') {
                delete d.bad;
                src = d.fromSource;
                tgt = d.fromTarget;
                if (src && !tgt) {
                    d.bad = 'missing link';
                } else if (src.type !== tgt.type) {
                    d.bad = 'type mismatch';
                }
                if (d.bad) {
                    lnks.push(d);
                }
            }
        });
        return lnks;
    }

    // ==========================
    // Module definition

    angular.module('ovTopo')
    .factory('TopoModelService',
        ['$log', 'FnService', 'RandomService',

        function (_$log_, _fs_, _rnd_) {
            $log = _$log_;
            fs = _fs_;
            rnd = _rnd_;

            function initModel(_api_, _dim_) {
                api = _api_;
                dim = _dim_;
                lu = api.network.lookup;
                rlk = api.network.revLinkToKey;
                nodes = api.network.nodes;
                links = api.network.links;
                linksByDevice = api.network.linksByDevice;
            }

            function newDim(_dim_) {
                dim = _dim_;
            }

            function destroyModel() { }

            return {
                initModel: initModel,
                newDim: newDim,
                destroyModel: destroyModel,

                positionNode: positionNode,
                resetAllLocations: resetAllLocations,
                createDeviceNode: createDeviceNode,
                createHostNode: createHostNode,
                createHostLink: createHostLink,
                createLink: createLink,
                coordFromLngLat: coordFromLngLat,
                lngLatFromCoord: lngLatFromCoord,
                findLink: findLink,
                findLinkById: findLinkById,
                findDevices: findDevices,
                findHosts: findHosts,
                findAttachedHosts: findAttachedHosts,
                findAttachedLinks: findAttachedLinks,
                findBadLinks: findBadLinks,
            };
        }]);
}());
