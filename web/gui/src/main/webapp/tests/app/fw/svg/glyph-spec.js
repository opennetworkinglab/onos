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
 ONOS GUI -- SVG -- Glyph Service - Unit Tests
 */

describe('factory: fw/svg/glyph.js', function() {
    var $log, fs, gs, d3Elem, svg;

    var numBaseGlyphs = 81,
        vbBird = '352 224 113 112',
        vbGlyph = '0 0 110 110',
        vbBadge = '0 0 10 10',
        longPrefix = 'M95.8,9.2H14.2c-2.8,0-5,2.2-5,5v81.5c0,2.8,2.2,5,5,' +
            '5h81.5c2.8,0,5-2.2,5-5V14.2C100.8,11.5,98.5,9.2,95.8,9.2z ',
        tablePrefix = 'M15.9,19.1h-8v-13h8V19.1z M90.5,6.1H75.6v13h14.9V6.1' +
            'z M71.9,6.1H56.9v13h14.9V6.1z M53.2,6.1H38.3v13h14.9V6.1z M34.5,' +
            '6.1H19.6v13h14.9V6.1z M102.2,6.1h-8v13h8V6.1z ',
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
            topo: 'M97.2,76.3H86.6',
            refresh: 'M102.6,40.8L88.4',
            garbage: 'M94.6,20.2c',

            // navigation specific glyphs
            flowTable: tablePrefix + 'M102.2,23.6H7.9v',
            portTable: tablePrefix + 'M102.6,23.6v78.5H',
            groupTable: 'M16,19.1H8v-13h',

            // toolbar specific glyphs
            summary: longPrefix + 'M16.7',
            details: longPrefix + 'M16.9',
            ports: 'M98,9.2H79.6c',
            map: 'M95.8,9.2H14.2c-2.8,0-5,2.2-5,5v66',
            cycleLabels: 'M72.5,33.9c',
            oblique: 'M80.9,30.2h',
            filters: 'M24.8,13.3L',
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
            checkMark: 'M8.6,3.4L4',
            xMark: 'M7.8,6.7L6.7',
            triangleUp: 'M0.5,6.2c0',
            triangleDown: 'M9.5,4.2c0',
            plus: 'M4,2h2v2h2v2',
            minus: 'M2,4h6v2',
            play: 'M3,1.5l3.5,3.5',
            stop: 'M2.5,2.5h5',
            xClose: 'M8.6,6.8L6.8,8.6',

            cloud: 'M37.6,79.5c-6.9,8.7-20.4,8.6',

            // our test ones..
            triangle: 'M.5,.2',
            diamond: 'M.2,.5'
        },
        glyphIds = [
            'unknown', 'uiAttached',
            'node', 'switch', 'roadm', 'endstation', 'router',
            'bgpSpeaker', 'chain', 'crown', 'lock', 'topo', 'refresh',
            'garbage',
            'flowTable', 'portTable', 'groupTable',
            'summary', 'details', 'ports', 'map', 'cycleLabels',
            'oblique', 'filters', 'resetZoom', 'relatedIntents', 'nextIntent',
            'prevIntent', 'intentTraffic', 'allTraffic', 'flows', 'eqMaster'
        ],
        badgeIds = [
            'checkMark', 'xMark', 'triangleUp', 'triangleDown',
            'plus', 'minus', 'play', 'stop'
        ],
        spriteIds = [
            'cloud'
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
            'clear', 'init', 'registerGlyphs', 'registerGlyphSet',
            'ids', 'glyph', 'glyphDefined', 'loadDefs', 'addGlyph'
        ])).toBe(true);
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
        expect(fs.contains(gs.ids(), id)).toBe(true);
        expect(glyph).toBeDefined();
        expect(glyph.id).toEqual(id);
        expect(glyph.vb).toEqual(vbox);
        expect(glyph.d.slice(0, plen)).toEqual(prefix);
    }

    xit('should be configured with the correct number of glyphs', function () {
        var nGlyphs = 1 + glyphIds.length + badgeIds.length + spriteIds.length;
        expect(nGlyphs).toEqual(numBaseGlyphs);
    });

    it('should load the bird glyph', function() {
        gs.init();
        verifyGlyphLoadedInCache('bird', vbBird);
    });

    xit('should load the regular glyphs', function () {
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

    it('should load the sprites', function () {
        gs.init();
        spriteIds.forEach(function (id) {
            verifyGlyphLoadedInCache(id, vbGlyph);
        });
    });


    // define some glyphs that we want to install

    var testVbox = '0 0 1 1',
        triVbox = '0 0 12 12',
        diaVbox = '0 0 15 15',
        dTriangle = 'M.5,.2l.3,.6,h-.6z',
        dDiamond = 'M.2,.5l.3,-.3l.3,.3l-.3,.3z',
        newGlyphs = {
            _viewbox: testVbox,
            triangle: dTriangle,
            diamond: dDiamond
        },
        dupGlyphs = {
            _viewbox: testVbox,
            router: dTriangle,
            switch: dDiamond
        },
        altNewGlyphs = {
            _triangle: triVbox,
            triangle: dTriangle,
            _diamond: diaVbox,
            diamond: dDiamond
        },
        altDupGlyphs = {
            _router: triVbox,
            router: dTriangle,
            _switch: diaVbox,
            switch: dDiamond
        },
        badGlyphSet = {
            triangle: dTriangle,
            diamond: dDiamond
        },
        warnMsg = 'GlyphService.registerGlyphs(): ',
        warnMsgSet = 'GlyphService.registerGlyphSet(): ',
        idCollision = warnMsg + 'ID collision: ',
        idCollisionSet = warnMsgSet + 'ID collision: ',
        missVbSet = warnMsgSet + 'no "_viewbox" property found',
        missVbCustom = warnMsg + 'Missing viewbox property: ',
        missVbTri = missVbCustom + '"_triangle"',
        missVbDia = missVbCustom + '"_diamond"';


    it('should install new glyphs as a glyph-set', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
        spyOn($log, 'warn');

        var ok = gs.registerGlyphSet(newGlyphs);
        expect(ok).toBe(true);
        expect($log.warn).not.toHaveBeenCalled();

        expect(gs.ids().length).toEqual(numBaseGlyphs + 2);
        verifyGlyphLoadedInCache('triangle', testVbox);
        verifyGlyphLoadedInCache('diamond', testVbox);
    });

    it('should not overwrite glyphs (via glyph-set) with dup IDs', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
        spyOn($log, 'warn');

        var ok = gs.registerGlyphSet(dupGlyphs);
        expect(ok).toBe(false);
        expect($log.warn).toHaveBeenCalledWith(idCollisionSet + '"switch"');
        expect($log.warn).toHaveBeenCalledWith(idCollisionSet + '"router"');

        expect(gs.ids().length).toEqual(numBaseGlyphs);
        // verify original glyphs still exist...
        verifyGlyphLoadedInCache('router', vbGlyph);
        verifyGlyphLoadedInCache('switch', vbGlyph);
    });

    it('should replace glyphs (via glyph-set) if asked nicely', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
        spyOn($log, 'warn');

        var ok = gs.registerGlyphSet(dupGlyphs, true);
        expect(ok).toBe(true);
        expect($log.warn).not.toHaveBeenCalled();

        expect(gs.ids().length).toEqual(numBaseGlyphs);
        // verify glyphs have been overwritten...
        verifyGlyphLoadedInCache('router', testVbox, 'triangle');
        verifyGlyphLoadedInCache('switch', testVbox, 'diamond');
    });

    it ('should complain if missing _viewbox in a glyph-set', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
        spyOn($log, 'warn');

        var ok = gs.registerGlyphSet(badGlyphSet);
        expect(ok).toBe(false);
        expect($log.warn).toHaveBeenCalledWith(missVbSet);
        expect(gs.ids().length).toEqual(numBaseGlyphs);
    });

    it('should install new glyphs', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
        spyOn($log, 'warn');

        var ok = gs.registerGlyphs(altNewGlyphs);
        expect(ok).toBe(true);
        expect($log.warn).not.toHaveBeenCalled();

        expect(gs.ids().length).toEqual(numBaseGlyphs + 2);
        verifyGlyphLoadedInCache('triangle', triVbox);
        verifyGlyphLoadedInCache('diamond', diaVbox);
    });

    it('should not overwrite glyphs with dup IDs', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
        spyOn($log, 'warn');

        var ok = gs.registerGlyphs(altDupGlyphs);
        expect(ok).toBe(false);
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

        var ok = gs.registerGlyphs(altDupGlyphs, true);
        expect(ok).toBe(true);
        expect($log.warn).not.toHaveBeenCalled();

        expect(gs.ids().length).toEqual(numBaseGlyphs);
        // verify glyphs have been overwritten...
        verifyGlyphLoadedInCache('router', triVbox, 'triangle');
        verifyGlyphLoadedInCache('switch', diaVbox, 'diamond');
    });

    it ('should complain if missing custom viewbox', function () {
        gs.init();
        expect(gs.ids().length).toEqual(numBaseGlyphs);
        spyOn($log, 'warn');

        var ok = gs.registerGlyphs(badGlyphSet);
        expect(ok).toBe(false);
        expect($log.warn).toHaveBeenCalledWith(missVbTri);
        expect($log.warn).toHaveBeenCalledWith(missVbDia);
        expect(gs.ids().length).toEqual(numBaseGlyphs);
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
        gs.registerGlyphSet(newGlyphs);
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
