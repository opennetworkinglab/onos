'use strict';

var path = require('path');
var express = require('express');
var app = express();

var conf = {
  paths: {
    root: '../../../../../'
  },
  port: '8182'
}

if (process.env.ONOS_EXTERNAL_APP_DIRS) {
    var external_apps = process.env.ONOS_EXTERNAL_APP_DIRS.replace(/\s/,'').split(',');
    external_apps.forEach(function(a, i){
        let [appName, appPath] = a.split(':');
        conf.paths[appName] = appPath;
    });
}

var httpProxyInit = function (baseDirs) {

  Object.keys(baseDirs).forEach(dir => {
    var d = path.isAbsolute(baseDirs[dir]) ? baseDirs[dir] : path.join(__dirname, baseDirs[dir]);
    app.use(express.static(d));
  });

  app.get('/', function (req, res) {
    res.send('Hello World!');
  });

  app.listen(conf.port, function () {
    console.log(`Dev server is up and listening on http://localhost:${conf.port}!`);
  });
};

httpProxyInit(conf.paths);



