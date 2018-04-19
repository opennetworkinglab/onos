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
import { FnService } from '../util/fn.service';
import { LogService } from '../../log.service';

/**
 * ONOS GUI -- SVG -- GeoData Service
 *
 * The GeoData Service facilitates the fetching and caching of TopoJSON data
 * from the server, as well as providing a way of creating a path generator
 * for that data, to be used to render the map in an SVG layer.
 *
 * A TopoData object can be fetched by ID. IDs that start with an asterisk
 * identify maps bundled with the GUI. IDs that do not start with an
 * asterisk are assumed to be URLs to externally provided data.
 *
 *   var topodata = GeoDataService.fetchTopoData('*continental-us');
 *
 * The path generator can then be created for that data-set:
 *
 *   var gen = GeoDataService.createPathGenerator(topodata, opts);
 *
 * opts is an optional argument that allows the override of default settings:
 *   {
 *       objectTag: 'states',
 *       projection: d3.geo.mercator(),
 *       logicalSize: 1000,
 *       mapFillScale: .95
 *   };
 *
 * The returned object (gen) comprises transformed data (TopoJSON -> GeoJSON),
 * the D3 path generator function, and the settings used ...
 *
 *  {
 *      geodata:  { ... },
 *      pathgen:  function (...) { ... },
 *      settings: { ... }
 *  }
 */
@Injectable()
export class GeoDataService {

    constructor(
        private fn: FnService,
//        private http: HttpService,
        private log: LogService
    ) {
        this.log.debug('GeoDataService constructed');
    }

}
