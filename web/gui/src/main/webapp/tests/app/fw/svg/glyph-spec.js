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
    var $log, fs, gs;

    var vbBird = '352 224 113 112',
        vbGlyph = '0 0 110 110',
        vbBadge = '0 0 10 10';

    beforeEach(module('onosUtil', 'onosSvg'));

    beforeEach(inject(function (_$log_, FnService, GlyphService) {
        $log = _$log_;
        fs = FnService;
        gs = GlyphService;
    }));

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
        expect(gs.ids().length).toEqual(11);
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
});
