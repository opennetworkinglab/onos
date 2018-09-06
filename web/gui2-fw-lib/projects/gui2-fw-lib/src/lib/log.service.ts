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

export abstract class Logger {
  debug: any;
  info: any;
  warn: any;
  error: any;
}

/**
 * ONOS GUI -- LogService
 * Inspired by https://robferguson.org/blog/2017/09/09/a-simple-logging-service-for-angular-4/
 */
@Injectable({
  providedIn: 'root',
})
export class LogService extends Logger {
  debug: any;
  info: any;
  warn: any;
  error: any;

  invokeConsoleMethod(type: string, args?: any): void {}
}
