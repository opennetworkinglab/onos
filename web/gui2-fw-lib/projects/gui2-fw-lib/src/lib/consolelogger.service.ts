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
import { Injectable } from '@angular/core';
import { environment } from '../environments/environment';
import { Logger } from './log.service';

export let isDebugMode: boolean = !environment.production;

const noop = (): any => undefined;

/**
 * ONOS GUI -- LogService
 * Inspired by https://robferguson.org/blog/2017/09/09/a-simple-logging-service-for-angular-4/
 */
@Injectable({
  providedIn: 'root',
})
export class ConsoleLoggerService implements Logger {

  get debug() {
    if (isDebugMode) {
      // tslint:disable-next-line:no-console
      return console.debug.bind(console);
    } else {
      return noop;
    }
  }

  get info() {
    if (isDebugMode) {
      // tslint:disable-next-line:no-console
      return console.info.bind(console);
    } else {
      return noop;
    }
  }

  get warn() {
    return console.warn.bind(console);
  }

  get error() {
    return console.error.bind(console);
  }

  invokeConsoleMethod(type: string, args?: any): void {
    const logFn: Function = (console)[type] || console.log || noop;
    logFn.apply(console, [args]);
  }
}
