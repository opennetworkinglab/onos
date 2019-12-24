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
import { Inject } from '@angular/core';
import { Directive } from '@angular/core';
import { FnService } from './util/fn.service';
import { LogService } from './log.service';
import { OnosService } from './onos.service';

/**
 * ONOS GUI -- Detect Browser Directive
 */
@Directive({
  selector: '[onosDetectBrowser]'
})
export class DetectBrowserDirective {
  constructor(
    private fs: FnService,
    private log: LogService,
    private onos: OnosService,

    // TODO: Change the any type to Window when https://github.com/angular/angular/issues/15640 is fixed.
    @Inject('Window') private w: any
  ) {
        const body: HTMLBodyElement = document.getElementsByTagName('body')[0];
//        let body = d3.select('body');
        let browser = '';
        if (fs.isChrome()) {
            browser = 'chrome';
        } else if (fs.isChromeHeadless()) {
            browser = 'chromeheadless';
        } else if (fs.isSafari()) {
            browser = 'safari';
        } else if (fs.isFirefox()) {
            browser = 'firefox';
        } else {
            this.log.warn('Unknown browser. ',
            'Vendor:', this.w.navigator.vendor,
            'Agent:', this.w.navigator.userAgent);
            return;
        }
        body.classList.add(browser);
//        body.classed(browser, true);
        this.onos.browser = browser;

        if (fs.isMobile()) {
            body.classList.add('mobile');
            this.onos.mobile = true;
        }

//        this.log.debug('Detected browser is', fs.cap(browser));
    }
}
