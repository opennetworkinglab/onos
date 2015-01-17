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

 @author Simon Hunt
 */
describe('factory: fw/svg/glyph.js', function() {
    var $log, fs, gs, d3Elem;

    var numBaseGlyphs = 11,
        vbBird = '352 224 113 112',
        vbGlyph = '0 0 110 110',
        vbBadge = '0 0 10 10',
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
            uiAttached: 'M2,2.5a.5,.5',

            // our test ones..
            triangle: 'M.5,.2',
            diamond: 'M.2,.5'
        };

    beforeEach(module('onosUtil', 'onosSvg'));

    beforeEach(inject(function (_$log_, FnService, GlyphService) {
        $log = _$log_;
        fs = FnService;
        gs = GlyphService;
        d3Elem = d3.select('body').append('defs').attr('id', 'myDefs');
    }));

    afterEach(function () {
        d3.select('#myDefs').remove();
        gs.clear();
    });

    it('should define GlyphService', function () {
        expect(gs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(gs, [
            'clear', 'init', 'register', 'ids', 'glyph', 'loadDefs'
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

    it('should load the bird glyph', function() {
        gs.init();
        verifyGlyphLoadedInCache('bird', vbBird);
    });
    it('should load the unknown glyph', function() {
        gs.init();
        verifyGlyphLoadedInCache('unknown', vbGlyph);
    });
    it('should load the node glyph', function() {
        gs.init();
        verifyGlyphLoadedInCache('node', vbGlyph);
    });
    it('should load the switch glyph', function() {
        gs.init();
        verifyGlyphLoadedInCache('switch', vbGlyph);
    });
    it('should load the roadm glyph', function() {
        gs.init();
        verifyGlyphLoadedInCache('roadm', vbGlyph);
    });
    it('should load the endstation glyph', function() {
        gs.init();
        verifyGlyphLoadedInCache('endstation', vbGlyph);
    });
    it('should load the router glyph', function() {
        gs.init();
        verifyGlyphLoadedInCache('router', vbGlyph);
    });
    it('should load the bgpSpeaker glyph', function() {
        gs.init();
        verifyGlyphLoadedInCache('bgpSpeaker', vbGlyph);
    });
    it('should load the chain glyph', function() {
        gs.init();
        verifyGlyphLoadedInCache('chain', vbGlyph);
    });
    it('should load the crown glyph', function() {
        gs.init();
        verifyGlyphLoadedInCache('crown', vbGlyph);
    });
    it('should load the uiAttached glyph', function() {
        gs.init();
        verifyGlyphLoadedInCache('uiAttached', vbBadge);
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
});
