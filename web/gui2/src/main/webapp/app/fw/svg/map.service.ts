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
import { Injectable } from '@angular/core';
import { GlyphDataService } from './glyphdata.service';
import { LogService } from '../../log.service';

/**
 * ONOS GUI -- SVG -- Map Service
 *
 * The Map Service provides a simple API for loading geographical maps into
 * an SVG layer. For example, as a background to the Topology View.
 *
 * e.g.  var promise = MapService.loadMapInto(svgLayer, '*continental-us');
 *
 * The Map Service makes use of the GeoDataService to load the required data
 * from the server and to create the appropriate geographical projection.
 *
 * A promise is returned to the caller, which is resolved with the
 *  map projection once created.
 */
@Injectable()
export class MapService {

    constructor(
        private gds: GlyphDataService,
//        private q: ??QService??,
        private log: LogService
    ) {
        this.log.debug('MapService constructed');
    }

}
