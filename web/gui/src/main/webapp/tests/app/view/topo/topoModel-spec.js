/*
 * Copyright 2015-present Open Networking Laboratory
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
 ONOS GUI -- Topo View -- Topo Model Service - Unit Tests
 */
describe('factory: view/topo/topoModel.js', function() {
    var $log, fs, rnd, tms;

    // stop random numbers from being quite so random
    var mockRandom = {
        // mock spread returns s + 1
        spread: function (s) {
            return s + 1;
        },
        // mock random dimension returns d / 2 - 1
        randDim: function (d) {
            return d/2 - 1;
        },
        mock: 'yup'
    };

    // to mock out the [longOrX,latOrY] <=> [x,y] transformations, we will
    // add/subtract 2000, 3000 respectively:
    //   longOrX:2005 === x:5,   latOrY:3004 === y:4

    var mockProjection = function (lnglat) {
        return [lnglat[0] - 2000, lnglat[1] - 3000];
    };

    mockProjection.invert = function (xy) {
        return [xy[0] + 2000, xy[1] + 3000];
    };

    // our test devices and hosts:
    var dev1 = {
            'class': 'device',
            id: 'dev1',
            x: 17,
            y: 27,
            online: true
        },
        dev2 = {
            'class': 'device',
            id: 'dev2',
            x: 18,
            y: 28,
            online: true
        },
        host1 = {
            'class': 'host',
            id: 'host1',
            x: 23,
            y: 33,
            cp: {
                device: 'dev1',
                port: 7
            },
            ingress: 'dev1/7-host1'
        },
        host2 = {
            'class': 'host',
            id: 'host2',
            x: 24,
            y: 34,
            cp: {
                device: 'dev0',
                port: 0
            },
            ingress: 'dev0/0-host2'
        };


    // our test api
    var api = {
        projection: function () { return mockProjection; },
        network: {
            nodes: [dev1, dev2, host1, host2],
            links: [],
            lookup: {dev1: dev1, dev2: dev2, host1: host1, host2: host2},
            revLinkToKey: {}
        },
        restyleLinkElement: function () {},
        removeLinkElement: function () {}
    };

    // our test dimensions and well known locations..
    var dim = [20, 40],
        randLoc = [9, 19],          // random location using randDim(): d/2-1
        randHostLoc = [40, 50],     // host "near" random location
                                    //  given that 'nearDist' = 15
                                    //  and spread(15) = 16
                                    //  9 + 15 + 16 = 40; 19 + 15 + 16 = 50
        nearDev1 = [48,58],         // [17+15+16, 27+15+16]
        dev1Loc = [17,27],
        dev2Loc = [18,28],
        host1Loc = [23,33],
        host2Loc = [24,34];

    // implement some custom matchers...
    beforeEach(function () {
        jasmine.addMatchers({
            toBePositionedAt: function () {
                return {
                    compare: function (actual, xy) {
                        var result = {},
                            actCoord = [actual.x, actual.y];

                        result.pass = (actual.x === xy[0]) && (actual.y === xy[1]);

                        if (result.pass) {
                            // for negation with ".not"
                            result.message = 'Expected [' + actCoord +
                            '] NOT to be positioned at [' + xy + ']';
                        } else {
                            result.message = 'Expected [' + actCoord +
                            '] to be positioned at [' + xy + ']';
                        }
                        return result;
                    }
                }
            },
            toHaveEndPoints: function () {
                return {
                    compare: function (actual, xy1, xy2) {
                        var result = {};

                        result.pass = (actual.source.x === xy1[0]) &&
                                      (actual.source.y === xy1[1]) &&
                                      (actual.target.x === xy2[0]) &&
                                      (actual.target.y === xy2[1]);

                        if (result.pass) {
                            // for negation with ".not"
                            result.message = 'Expected ' + actual +
                            ' NOT to have endpoints [' + xy1 + ']-[' + xy2 + ']';
                        } else {
                            result.message = 'Expected ' + actual +
                            ' to have endpoints [' + xy1 + ']-[' + xy2 + ']';
                        }
                        return result;
                    }
                }
            },
            toBeFixed: function () {
                return {
                    compare: function (actual) {
                        var result = {
                            pass: actual.fixed
                        };
                        if (result.pass) {
                            result.message = 'Expected ' + actual +
                            ' NOT to be fixed!';
                        } else {
                            result.message = 'Expected ' + actual +
                            ' to be fixed!';
                        }
                        return result;
                    }
                }
            }
        });
    });

    beforeEach(module('ovTopo', 'onosUtil', 'onosNav', 'onosLayer', 'onosWidget', 'onosMast'));

    beforeEach(function () {
        module(function ($provide) {
            $provide.value('RandomService', mockRandom);
        });
    });

    beforeEach(inject(function (_$log_, FnService, RandomService, TopoModelService) {
        $log = _$log_;
        fs = FnService;
        rnd = RandomService;
        tms = TopoModelService;
        tms.initModel(api, dim);
    }));


    it('should install the mock random service', function () {
        expect(rnd.mock).toBe('yup');
        expect(rnd.spread(4)).toBe(5);
        expect(rnd.randDim(8)).toBe(3);
    });

    it('should install the mock projection', function () {
        expect(tms.coordFromLngLat({longOrX: 2005, latOrY: 3004})).toEqual([5,4]);
        expect(tms.lngLatFromCoord([5,4])).toEqual([2005,3004]);
    });

    it('should define TopoModelService', function () {
        expect(tms).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(tms, [
            'initModel', 'newDim', 'destroyModel',
            'positionNode', 'resetAllLocations',
            'createDeviceNode', 'createHostNode',
            'createHostLink', 'createLink',
            'coordFromLngLat', 'lngLatFromCoord',
            'findLink', 'findLinkById', 'findDevices', 'findHosts',
            'findAttachedHosts', 'findAttachedLinks', 'findBadLinks'
        ])).toBeTruthy();
    });

    // === unit tests for positionNode()

    it('should position a node using meta x/y', function () {
        var node = {
            metaUi: { x:37, y:48 }
        };
        tms.positionNode(node);
        expect(node).toBePositionedAt([37,48]);
        expect(node).toBeFixed();
    });

    it('should position a node by translating lng/lat', function () {
        var node = {
            location: {
                locType: 'geo',
                longOrX: 2008,
                latOrY: 3009
            }
        };
        tms.positionNode(node);
        expect(node).toBePositionedAt([8,9]);
        expect(node).toBeFixed();
    });

    it('should position a device with no location randomly', function () {
        var node = { 'class': 'device' };
        tms.positionNode(node);
        expect(node).toBePositionedAt(randLoc);
        expect(node).not.toBeFixed();
    });

    it('should position a device randomly even if x/y set', function () {
        var node = { 'class': 'device', x: 1, y: 2 };
        tms.positionNode(node);
        expect(node).toBePositionedAt(randLoc);
        expect(node).not.toBeFixed();
    });

    it('should NOT reposition a device randomly on update', function () {
        var node = { 'class': 'device', x: 1, y: 2 };
        tms.positionNode(node, true);
        expect(node).toBePositionedAt([1,2]);
        expect(node).not.toBeFixed();
    });

    it('should position a host close to its device', function () {
        var node = { 'class': 'host', cp: { device: 'dev1' } };
        tms.positionNode(node);

        // note: nearDist is 15; spread(15) adds 16; dev1 at [17,27]

        expect(node).toBePositionedAt(nearDev1);
        expect(node).not.toBeFixed();
    });

    it('should randomize host with no assoc device', function () {
        var node = { 'class': 'host', cp: { device: 'dev0' } };
        tms.positionNode(node);

        // note: no device gives 'rand loc' [9,19]
        //       nearDist is 15; spread(15) adds 16

        expect(node).toBePositionedAt(randHostLoc);
        expect(node).not.toBeFixed();
    });

    // === unit tests for createDeviceNode()

    it('should create a basic device node', function () {
        var node = tms.createDeviceNode({ id: 'foo' });
        expect(node).toBePositionedAt(randLoc);
        expect(node).not.toBeFixed();
        expect(node.class).toEqual('device');
        expect(node.svgClass).toEqual('node device');
        expect(node.id).toEqual('foo');
    });

    it('should create device node with type', function () {
        var node = tms.createDeviceNode({ id: 'foo', type: 'cool' });
        expect(node).toBePositionedAt(randLoc);
        expect(node).not.toBeFixed();
        expect(node.class).toEqual('device');
        expect(node.svgClass).toEqual('node device cool');
        expect(node.id).toEqual('foo');
    });

    it('should create online device node with type', function () {
        var node = tms.createDeviceNode({ id: 'foo', type: 'cool', online: true });
        expect(node).toBePositionedAt(randLoc);
        expect(node).not.toBeFixed();
        expect(node.class).toEqual('device');
        expect(node.svgClass).toEqual('node device cool online');
        expect(node.id).toEqual('foo');
    });

    it('should create online device node with type and lng/lat', function () {
        var node = tms.createDeviceNode({
            id: 'foo',
            type: 'yowser',
            online: true,
            location: {
                locType: 'geo',
                longOrX: 2048,
                latOrY: 3096
            }
        });
        expect(node).toBePositionedAt([48,96]);
        expect(node).toBeFixed();
        expect(node.class).toEqual('device');
        expect(node.svgClass).toEqual('node device yowser online');
        expect(node.id).toEqual('foo');
    });

    // === unit tests for createHostNode()

    it('should create a basic host node', function () {
        var node = tms.createHostNode({ id: 'bar', cp: { device: 'dev0' } });
        expect(node).toBePositionedAt(randHostLoc);
        expect(node).not.toBeFixed();
        expect(node.class).toEqual('host');
        expect(node.svgClass).toEqual('node host endstation');
        expect(node.id).toEqual('bar');
    });

    it('should create a host with type', function () {
        var node = tms.createHostNode({
            id: 'bar',
            type: 'classic',
            cp: { device: 'dev1' }
        });
        expect(node).toBePositionedAt(nearDev1);
        expect(node).not.toBeFixed();
        expect(node.class).toEqual('host');
        expect(node.svgClass).toEqual('node host classic');
        expect(node.id).toEqual('bar');
    });

    // === unit tests for createHostLink()

    it('should create a basic host link', function () {
        var link = tms.createHostLink(host1);
        expect(link.source).toEqual(host1);
        expect(link.target).toEqual(dev1);
        expect(link).toHaveEndPoints(host1Loc, dev1Loc);
        expect(link.key).toEqual('dev1/7-host1');
        expect(link.class).toEqual('link');
        expect(link.type()).toEqual('hostLink');
        expect(link.linkWidth()).toEqual(1);
        expect(link.online()).toEqual(true);
    });

    it('should return null for failed endpoint lookup', function () {
        spyOn($log, 'error');
        var link = tms.createHostLink(host2);
        expect(link).toBeNull();
        expect($log.error).toHaveBeenCalledWith(
            'Node(s) not on map for link:\n[dst] "dev0" missing'
        );
    });

    // === unit tests for createLink()

    it('should return null for missing endpoints', function () {
        spyOn($log, 'error');
        var link = tms.createLink({src: 'dev0', dst: 'dev00'});
        expect(link).toBeNull();
        expect($log.error).toHaveBeenCalledWith(
            'Node(s) not on map for link:\n[src] "dev0" missing\n[dst] "dev00" missing'
        );
    });

    xit('should create a basic link', function () {
        var linkData = {
                src: 'dev1',
                dst: 'dev2',
                id: 'baz',
                type: 'zoo',
                online: true,
                linkWidth: 1.5
            },
            link = tms.createLink(linkData);
        expect(link.source).toEqual(dev1);
        expect(link.target).toEqual(dev2);
        expect(link).toHaveEndPoints(dev1Loc, dev2Loc);
        expect(link.key).toEqual('baz');
        expect(link.class).toEqual('link');
        expect(link.fromSource).toBe(linkData);
        expect(link.type()).toEqual('zoo');
        expect(link.online()).toEqual(true); // this is the condition failing
        expect(link.linkWidth()).toEqual(1.5);
    });

    // TODO: more unit tests for additional functions....
});
