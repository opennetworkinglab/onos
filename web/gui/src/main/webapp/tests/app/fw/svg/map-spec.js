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
 ONOS GUI -- SVG -- Map Service - Unit Tests

 @author Simon Hunt
 */
describe('factory: fw/svg/map.js', function() {
    var ms;

    beforeEach(module('onosSvg'));

    beforeEach(inject(function (MapService) {
        ms = MapService;
    }));

    it('should define MapService', function () {
        expect(ms).toBeDefined();
    });

    // TODO: unit tests for map functions
});
