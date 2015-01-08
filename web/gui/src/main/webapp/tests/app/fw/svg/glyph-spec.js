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
        vbBadge = '0 0 10 10';

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
            'init', 'register', 'ids', 'glyph', 'loadDefs'
        ])).toBeTruthy();
    });

    it('should start with no glyphs loaded', function () {
        expect(gs.ids()).toEqual([]);
    });

    it('should load the base set of glyphs', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
    });

    it('should remove glyphs on clear', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
        gs.clear();
        expect(gs.ids().length).toEqual(0);
    });

    function verifyGlyphLoaded(id, vbox, prefix) {
        var glyph = gs.glyph(id),
            plen = prefix.length;
        expect(fs.contains(gs.ids(), id)).toBeTruthy();
        expect(glyph).toBeDefined();
        expect(glyph.id).toEqual(id);
        expect(glyph.vb).toEqual(vbox);
        expect(glyph.d.slice(0, plen)).toEqual(prefix);
    }

    it('should load the bird glyph', function() {
        gs.init();
        verifyGlyphLoaded('bird', vbBird, 'M427.7,300.4');
    });
    it('should load the unknown glyph', function() {
        gs.init();
        verifyGlyphLoaded('unknown', vbGlyph, 'M35,40a5');
    });
    it('should load the node glyph', function() {
        gs.init();
        verifyGlyphLoaded('node', vbGlyph, 'M15,100a5');
    });
    it('should load the switch glyph', function() {
        gs.init();
        verifyGlyphLoaded('switch', vbGlyph, 'M10,20a10');
    });
    it('should load the roadm glyph', function() {
        gs.init();
        verifyGlyphLoaded('roadm', vbGlyph, 'M10,35l25-');
    });
    it('should load the endstation glyph', function() {
        gs.init();
        verifyGlyphLoaded('endstation', vbGlyph, 'M10,15a5,5');
    });
    it('should load the router glyph', function() {
        gs.init();
        verifyGlyphLoaded('router', vbGlyph, 'M10,55A45,45');
    });
    it('should load the bgpSpeaker glyph', function() {
        gs.init();
        verifyGlyphLoaded('bgpSpeaker', vbGlyph, 'M10,40a45,35');
    });
    it('should load the chain glyph', function() {
        gs.init();
        verifyGlyphLoaded('chain', vbGlyph, 'M60.4,77.6c-');
    });
    it('should load the crown glyph', function() {
        gs.init();
        verifyGlyphLoaded('crown', vbGlyph, 'M99.5,21.6c0');
    });
    it('should load the uiAttached glyph', function() {
        gs.init();
        verifyGlyphLoaded('uiAttached', vbBadge, 'M2,2.5a.5,.5');
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
        verifyGlyphLoaded('triangle', testVbox, 'M.5,.2');
        verifyGlyphLoaded('diamond', testVbox, 'M.2,.5');
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
        verifyGlyphLoaded('router', vbGlyph, 'M10,55A45,45');
        verifyGlyphLoaded('switch', vbGlyph, 'M10,20a10');
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
        verifyGlyphLoaded('router', testVbox, 'M.5,.2');
        verifyGlyphLoaded('switch', testVbox, 'M.2,.5');
    });

    function verifyPathPrefix(elem, prefix) {
        var plen = prefix.length,
            d = elem.select('path').attr('d');
        expect(d.slice(0, plen)).toEqual(prefix);
    }

    it('should load base glyphs into the DOM', function () {
        gs.init();
        gs.loadDefs(d3Elem);
        expect(d3Elem.selectAll('symbol').size()).toEqual(numBaseGlyphs);

        // verify bgpSpeaker
        var bs = d3Elem.select('#bgpSpeaker');
        expect(bs.size()).toEqual(1);
        expect(bs.attr('viewBox')).toEqual(vbGlyph);
        verifyPathPrefix(bs, 'M10,40a45,35');
    });

    it('should load custom glyphs into the DOM', function () {
        gs.init();
        gs.register(testVbox, newGlyphs);
        gs.loadDefs(d3Elem);
        expect(d3Elem.selectAll('symbol').size()).toEqual(numBaseGlyphs + 2);

        // verify diamond
        var dia = d3Elem.select('#diamond');
        expect(dia.size()).toEqual(1);
        expect(dia.attr('viewBox')).toEqual(testVbox);
        verifyPathPrefix(dia, 'M.2,.5l.3,-.3');
    });
});
