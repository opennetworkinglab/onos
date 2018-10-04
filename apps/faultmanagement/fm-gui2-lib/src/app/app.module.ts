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

import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { RouterModule, Routes }  from '@angular/router';
import { FmGui2LibModule } from 'fm-gui2-lib';
import { AppComponent } from './app.component';
import { Gui2FwLibModule, ConsoleLoggerService, LogService } from 'gui2-fw-lib';

const appRoutes: Routes = [
  { path: '**', component: AppComponent }
]

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    RouterModule.forRoot(appRoutes),
    BrowserModule,
    FmGui2LibModule,
    Gui2FwLibModule
  ],
  providers: [
    { provide: 'Window', useValue: window },
    { provide: LogService, useClass: ConsoleLoggerService },
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
