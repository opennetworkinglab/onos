/*
 * Copyright 2018-present Open Networking Foundation
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
import { Component, OnInit, OnDestroy } from '@angular/core';
import { trigger, state, style, animate, transition } from '@angular/animations';

import {
    LionService,
    LogService,
    NavService,
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

/**
 * ONOS GUI -- Navigation Module
 *
 * Note: While this NavComponent could arguably be moved to the gui2-fw-lib
 * it brings problems in recognizing the "routerlink" directives as being part
 * of this application. So for that reason Nav must remain here for routing to work
 */
@Component({
  selector: 'onos-nav',
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.theme.css', './nav.component.css'],
  animations: [
    trigger('navState', [
      state('false', style({
        transform: 'translateY(-100%)'
      })),
      state('true', style({
        transform: 'translateY(0%)'
      })),
      transition('0 => 1', animate('100ms ease-in')),
      transition('1 => 0', animate('100ms ease-out'))
    ])
  ]
})
export class NavComponent implements OnInit, OnDestroy {
    lionFn; // Function

    constructor(
        private log: LogService,
        private lion: LionService,
        public ns: NavService,
    ) {
        this.log.debug('NavComponent constructed');
    }

    /**
     * If LION is not ready we make do with a dummy function
     * As soon a lion gets loaded this function will be replaced with
     * the real thing
     */
    ngOnInit() {
        if (this.lion.ubercache.length === 0) {
            this.lionFn = this.dummyLion;
            this.lion.loadCbs.set('nav', () => this.doLion());
            this.log.debug('LION not available when NavComponent initialized');
        } else {
            this.doLion();
        }
        this.ns.getUiViews();
    }

    /**
     * Nav component should never be closed, but in case it does, it's
     * safer to tidy up after itself
     */
    ngOnDestroy() {
        this.lion.loadCbs.delete('nav');
    }

    /**
    * Read the LION bundle for App and set up the lionFn
    */
    doLion() {
        this.lionFn = this.lion.bundle('core.fw.Nav');
    }

    /**
    * A dummy implementation of the lionFn until the response is received and the LION
    * bundle is received from the WebSocket
    */
    dummyLion(key: string): string {
        return '%' + key + '%';
    }
}
