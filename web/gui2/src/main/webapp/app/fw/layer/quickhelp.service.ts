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
import { LionService } from '../util/lion.service';
import { LogService } from '../../log.service';
import { SvgUtilService } from '../svg/svgutil.service';

/**
 * ONOS GUI -- Layer -- Quick Help Service
 *
 * Provides a mechanism to display key bindings and mouse gesture notes.
 */
@Injectable({
  providedIn: 'root',
})
export class QuickHelpService {

  constructor(
    private fs: FnService,
    private ls: LionService,
    private log: LogService,
    private sus: SvgUtilService
  ) {
    this.log.debug('QuickhelpService constructed');
  }

}
