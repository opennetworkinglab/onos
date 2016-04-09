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
 ONOS GUI -- Util -- Random Service - Unit Tests
 */
describe('factory: fw/util/random.js', function() {
    var rnd, $log, fs;

    beforeEach(module('onosUtil'));

    beforeEach(inject(function (RandomService, _$log_, FnService) {
        rnd = RandomService;
        $log = _$log_;
        fs = FnService;
    }));

    // interesting use of a custom matcher...
    beforeEach(function () {
        jasmine.addMatchers({
            toBeWithinOf: function () {
                return {
                    compare: function (actual, distance, base) {
                        var lower = base - distance,
                            upper = base + distance,
                            result = {};

                        result.pass = Math.abs(actual - base) <= distance;

                        if (result.pass) {
                            // for negation with ".not"
                            result.message = 'Expected ' + actual +
                                ' to be outside ' + lower + ' and ' +
                                upper + ' (inclusive)';
                        } else {
                            result.message = 'Expected ' + actual +
                            ' to be between ' + lower + ' and ' +
                            upper + ' (inclusive)';
                        }
                        return result;
                    }
                }
            }
        });
    });

    it('should define RandomService', function () {
        expect(rnd).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(rnd, [
            'spread', 'randDim'
        ])).toBeTruthy();
    });

    // really, can only do this heuristically.. hope this doesn't break
    it('should spread results across the range', function () {
        var load = 1000,
            s = 12,
            low = 0,
            high = 0,
            i, res,
            which = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            minCount = load / s * 0.5;  // generous error

        for (i=0; i<load; i++) {
            res = rnd.spread(s);
            if (res < low) low = res;
            if (res > high) high = res;
            which[res + s/2]++;
        }
        expect(low).toBe(-6);
        expect(high).toBe(5);

        // check we got a good number of hits in each bucket
        for (i=0; i<s; i++) {
            expect(which[i]).toBeGreaterThan(minCount);
        }
    });

    // really, can only do this heuristically.. hope this doesn't break
    it('should choose results across the dimension', function () {
        var load = 1000,
            dim = 100,
            low = 999,
            high = 0,
            i, res;

        for (i=0; i<load; i++) {
            res = rnd.randDim(dim);
            if (res < low) low = res;
            if (res > high) high = res;
            expect(res).toBeWithinOf(36, 50);
        }
    });
});
