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
 ONOS GUI -- SVG -- Glyph Service - Unit Tests
 */
describe('factory: fw/svg/glyph.js', function() {
    var $log, fs, gs, d3Elem, svg;

    var numBaseGlyphs = 34,
        vbBird = '352 224 113 112',
        vbGlyph = '0 0 110 110',
        vbBadge = '0 0 10 10',
        longPrefix = 'M95.8,9.2H14.2c-2.8,0-5,2.2-5,5v81.5c0,2.8,2.2,5,5,' +
            '5h81.5c2.8,0,5-2.2,5-5V14.2C100.8,11.5,98.5,9.2,95.8,9.2z ',
        prefixLookup = {
            bird: 'M427.7,300.4',
            unknown: 'M35,40a5',
            node: 'M15,100a5',
            switch: 'M10,20a10',
            roadm: 'M10,35l25-',
            endstation: 'M10,15a5,5',
            router: 'M10,55A45,45',
            bgpSpeaker: 'M10,40a45,35',
            chain: 'M60.4,77.6c-',
            crown: 'M99.5,21.6c0,',
            lock: 'M79.4,48.6h',

            // toolbar specific glyphs
            summary: longPrefix + 'M16.7',
            details: longPrefix + 'M16.9',
            ports: 'M98,9.2H79.6c',
            map: 'M95.8,9.2H14.2c-2.8,0-5,2.2-5,5v66',
            cycleLabels: 'M72.5,33.9c',
            oblique: 'M80.9,30.2h',
            resetZoom: 'M86,79.8L',
            relatedIntents: 'M99.9,43.7',
            nextIntent: 'M88.1,55.7',
            prevIntent: 'M22.5,55.6',
            intentTraffic: 'M14.7,71.5h',
            allTraffic: 'M15.7,64.5h-7v',
            flows: 'M93.8,46.1c',
            eqMaster: 'M100.1,46.9l',

            // badges
            uiAttached: 'M2,2.5a.5,.5',
            checkMark: 'M2.6,4.5c0',
            xMark: 'M9.0,7.2C8.2',
            triangleUp: 'M0.5,6.2c0',
            triangleDown: 'M9.5,4.2c0',
            plus: 'M4,2h2v2h2v2',
            minus: 'M2,4h6v2',
            play: 'M2.5,2l5.5,3',
            stop: 'M2.5,2.5h5',

            // our test ones..
            triangle: 'M.5,.2',
            diamond: 'M.2,.5'
        },
        glyphIds = [
            'unknown', 'node', 'switch', 'roadm', 'endstation', 'router',
            'bgpSpeaker', 'chain', 'crown', 'lock',
            'summary', 'details', 'ports', 'map', 'cycleLabels', 'oblique',
            'resetZoom', 'relatedIntents', 'nextIntent', 'prevIntent',
            'intentTraffic', 'allTraffic', 'flows', 'eqMaster'
        ],
        badgeIds = [
            'uiAttached', 'checkMark', 'xMark', 'triangleUp', 'triangleDown',
            'plus', 'minus', 'play', 'stop'
        ];

    beforeEach(module('onosUtil', 'onosSvg'));

    beforeEach(inject(function (_$log_, FnService, GlyphService) {
        var body = d3.select('body');
        $log = _$log_;
        fs = FnService;
        gs = GlyphService;
        d3Elem = body.append('defs').attr('id', 'myDefs');
        svg = body.append('svg').attr('id', 'mySvg');
    }));

    afterEach(function () {
        d3.select('#mySvg').remove();
        d3.select('#myDefs').remove();
        gs.clear();
    });

    it('should define GlyphService', function () {
        expect(gs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(gs, [
            'clear', 'init', 'register', 'ids', 'glyph', 'loadDefs', 'addGlyph'
        ])).toBeTruthy();
    });

    it('should start with no glyphs loaded', function () {
        expect(gs.ids()).toEqual([]);
    });

    it('should load the base set of glyphs into the cache', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
    });

    it('should remove glyphs from the cache on clear', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
        gs.clear();
        expect(gs.ids().length).toEqual(0);
    });

    function verifyGlyphLoadedInCache(id, vbox, expPfxId) {
        var pfxId = expPfxId || id,
            glyph = gs.glyph(id),
            prefix = prefixLookup[pfxId],
            plen = prefix.length;
        expect(fs.contains(gs.ids(), id)).toBeTruthy();
        expect(glyph).toBeDefined();
        expect(glyph.id).toEqual(id);
        expect(glyph.vb).toEqual(vbox);
        expect(glyph.d.slice(0, plen)).toEqual(prefix);
    }

    it('should be configured with the correct number of glyphs', function () {
        expect(1 + glyphIds.length + badgeIds.length).toEqual(numBaseGlyphs);
    });

    it('should load the bird glyph', function() {
        gs.init();
        verifyGlyphLoadedInCache('bird', vbBird);
    });

    it('should load the regular glyphs', function () {
        gs.init();
        glyphIds.forEach(function (id) {
            verifyGlyphLoadedInCache(id, vbGlyph);
        });
    });

    it('should load the badge glyphs', function () {
        gs.init();
        badgeIds.forEach(function (id) {
            verifyGlyphLoadedInCache(id, vbBadge);
        });
    });


    // define some glyphs that we want to install

    var testVbox = '0 0 1 1',
        dTriangle = 'M.5,.2l.3,.6,h-.6z',
        dDiamond = 'M.2,.5l.3,-.3l.3,.3l-.3,.3z',
        newGlyphs = {
            triangle: dTriangle,
            diamond: dDiamond
        },
        dupGlyphs = {
            router: dTriangle,
            switch: dDiamond
        },
        idCollision = 'GlyphService.register(): ID collision: ';

    it('should install new glyphs', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
        spyOn($log, 'warn');

        var ok = gs.register(testVbox, newGlyphs);
        expect(ok).toBeTruthy();
        expect($log.warn).not.toHaveBeenCalled();

        expect(gs.ids().length).toEqual(numBaseGlyphs + 2);
        verifyGlyphLoadedInCache('triangle', testVbox);
        verifyGlyphLoadedInCache('diamond', testVbox);
    });

    it('should not overwrite glyphs with dup IDs', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
        spyOn($log, 'warn');

        var ok = gs.register(testVbox, dupGlyphs);
        expect(ok).toBeFalsy();
        expect($log.warn).toHaveBeenCalledWith(idCollision + '"switch"');
        expect($log.warn).toHaveBeenCalledWith(idCollision + '"router"');

        expect(gs.ids().length).toEqual(numBaseGlyphs);
        // verify original glyphs still exist...
        verifyGlyphLoadedInCache('router', vbGlyph);
        verifyGlyphLoadedInCache('switch', vbGlyph);
    });

    it('should replace glyphs if asked nicely', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
        spyOn($log, 'warn');

        var ok = gs.register(testVbox, dupGlyphs, true);
        expect(ok).toBeTruthy();
        expect($log.warn).not.toHaveBeenCalled();

        expect(gs.ids().length).toEqual(numBaseGlyphs);
        // verify glyphs have been overwritten...
        verifyGlyphLoadedInCache('router', testVbox, 'triangle');
        verifyGlyphLoadedInCache('switch', testVbox, 'diamond');
    });

    function verifyPathPrefix(elem, prefix) {
        var plen = prefix.length,
            d = elem.select('path').attr('d');
        expect(d.slice(0, plen)).toEqual(prefix);
    }

    function verifyLoadedInDom(id, vb, expPfxId) {
        var pfxId = expPfxId || id,
            symbol = d3Elem.select('#' + id);
        expect(symbol.size()).toEqual(1);
        expect(symbol.attr('viewBox')).toEqual(vb);
        verifyPathPrefix(symbol, prefixLookup[pfxId]);
    }

    it('should load base glyphs into the DOM', function () {
        gs.init();
        gs.loadDefs(d3Elem);
        expect(d3Elem.selectAll('symbol').size()).toEqual(numBaseGlyphs);
        verifyLoadedInDom('bgpSpeaker', vbGlyph);
    });

    it('should load custom glyphs into the DOM', function () {
        gs.init();
        gs.register(testVbox, newGlyphs);
        gs.loadDefs(d3Elem);
        expect(d3Elem.selectAll('symbol').size()).toEqual(numBaseGlyphs + 2);
        verifyLoadedInDom('diamond', testVbox);
    });

    it('should load only specified glyphs into the DOM', function () {
        gs.init();
        gs.loadDefs(d3Elem, ['crown', 'chain', 'node']);
        expect(d3Elem.selectAll('symbol').size()).toEqual(3);
        verifyLoadedInDom('crown', vbGlyph);
        verifyLoadedInDom('chain', vbGlyph);
        verifyLoadedInDom('node', vbGlyph);
    });

    it('should add a glyph with default size', function () {
        gs.init();
        var retval = gs.addGlyph(svg, 'crown');
        var what = svg.selectAll('use');
        expect(what.size()).toEqual(1);
        expect(what.attr('width')).toEqual('40');
        expect(what.attr('height')).toEqual('40');
        expect(what.attr('xlink:href')).toEqual('#crown');
        expect(what.classed('glyph')).toBeTruthy();
        expect(what.classed('overlay')).toBeFalsy();

        // check a couple on retval, which should be the same thing..
        expect(retval.attr('xlink:href')).toEqual('#crown');
        expect(retval.classed('glyph')).toBeTruthy();
    });

    it('should add a glyph with given size', function () {
        gs.init();
        gs.addGlyph(svg, 'crown', 37);
        var what = svg.selectAll('use');
        expect(what.size()).toEqual(1);
        expect(what.attr('width')).toEqual('37');
        expect(what.attr('height')).toEqual('37');
        expect(what.attr('xlink:href')).toEqual('#crown');
        expect(what.classed('glyph')).toBeTruthy();
        expect(what.classed('overlay')).toBeFalsy();
    });

    it('should add a glyph marked as overlay', function () {
        gs.init();
        gs.addGlyph(svg, 'crown', 20, true);
        var what = svg.selectAll('use');
        expect(what.size()).toEqual(1);
        expect(what.attr('width')).toEqual('20');
        expect(what.attr('height')).toEqual('20');
        expect(what.attr('xlink:href')).toEqual('#crown');
        expect(what.classed('glyph')).toBeTruthy();
        expect(what.classed('overlay')).toBeTruthy();
    });
});
