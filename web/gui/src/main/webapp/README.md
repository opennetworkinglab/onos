# ONOS UI

## Development environment

To help with UI development we provide a dedicated environment that introduce an auto reload feature and allow you to change your javascript files without recompiling the application.

To get started:
- Be sure to have `Node Js` installed
- Enter `web/gui/src/main/webapp/` folder
- Run `npm install` to install required dependency
- Run `npm start` to open start the development environment

In the console you should see something like:

```
Dev server is up and listening on http://localhost: 8182
[BS] Proxying: http://localhost:8181
[BS] Access URLs:
 ----------------------------------
       Local: http://localhost:3000
    External: http://10.1.8.46:3000
 ----------------------------------
          UI: http://localhost:3002
 UI External: http://10.1.8.46:3002
 ----------------------------------
[BS] Watching files...
```

To open ONOS visit the local URL (eg: `http://localhost:3000`) plus `/onos/ui`
(eg: `http://localhost:3000/onos/ui`)

## Loading files from external applications

The UI development environment provide the ability to serve UI files
from an external forlder that can be specified with:
`ONOS_EXTERNAL_APP_DIRS="appName:path-to-the-first-folder" npm start`

Eg:
`ONOS_EXTERNAL_APP_DIRS="sampleCustom:../../meow/sample/meowster-sample/" npm start`

_Note that `ONOS_EXTERNAL_APP_DIRS` is an environment variable,so it can be set with_
_`export ONOS_EXTERNAL_APP_DIRS="sampleCustom:../../meow/sample/meowster-sample/"`_