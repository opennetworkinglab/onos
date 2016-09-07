'use strict';
/*
 |--------------------------------------------------------------------------
 | Browser-sync config file
 |--------------------------------------------------------------------------
 |
 | For up-to-date information about the options:
 |   http://www.browsersync.io/docs/options/
 |
 | There are more options than you see here, these are just the ones that are
 | set internally. See the website for more info.
 |
 |
 */
var fs = require('fs');
var httpProxy = require('http-proxy');

var proxy = httpProxy.createProxyServer({
  target: 'http://localhost:8182',
});

if (process.env.ONOS_EXTERNAL_APP_DIRS) {
    var external_apps = process.env.ONOS_EXTERNAL_APP_DIRS.replace(/\s/,'').split(',');
    external_apps = external_apps.reduce(function(dict, app){
        var pieces = app.split(':');
        var appName = pieces[0];
        var appPath = pieces[1];
        dict[appName] = appPath;
        return dict;
    }, {});
}

var defaultViews = fs.readdirSync('./app/view/');
var viewNameMatcher = new RegExp(/\/onos\/ui\/app\/view\/(.+)\/.+\.(?:js|css|html)/);

var checkExternalApp = function (url) {
    if(external_apps){
        for(var i = 0; i < Object.keys(external_apps).length; i++){
            var key = Object.keys(external_apps)[i];
            if(url.indexOf(key) !== -1){
                return key;
            };
        }
    }
    return false;
};

proxy.on('upgrade', function (req, socket, head) {
  console.log('[WS]: ', head);
  proxy.ws(req, socket, head);
});

proxy.on('error', function (error, req, res) {
  res.writeHead(500, {
    'Content-Type': 'text/plain'
  });
  console.error('[Proxy]', error);
});

module.exports = {
  'files': [
    './app/**/*.css',
    './app/**/*.js',
    './app/**/*.json',
    './app/**/*.html',
    './app/**/*.jpg',
    './app/**/*.png',
    './app/**/*.gif',
    '../../../../../apps/**/*.js',
    '../../../../../apps/**/*.html',
    '../../../../../apps/**/*.css'
  ],
  proxy: {
    target: "http://localhost:8181",
    ws: true,
    middleware: function (req, res, next) {

      var viewName = viewNameMatcher.exec(req.url);
      var isExternalApp = checkExternalApp(req.url);

      if (!!viewName && !isExternalApp && defaultViews.indexOf(viewName[1]) === -1) {
        // in this case it is an application that extend the view
        // so we redirect the request to the app folder in case of .js, .css, .html
        req.url = req.url.replace('/onos/ui/', '/apps/' + viewName[1] + '/app/src/main/resources/');
        proxy.web(req, res);
      }
      else if (isExternalApp) {
        // in this case it is an external application (not in ONOS source code) that extend the view
        // so we redirect the request to the app folder in case of .js, .css, .html
        req.url = req.url.replace('/onos/ui/', '/src/main/resources/');
        proxy.web(req, res);
      }
      // NOTE onos.js and index.html should not be proxied (require server side injection)
      else if (req.url.match(/(?:js|css|html)/) && req.url !== '/onos/ui/onos.js' && 
               req.url !== '/onos/ui/index.html' && req.url !== '/onos/ui/nav.html') {
        // redirect onos base js files to the source folder
        req.url = req.url.replace('/onos/ui/', '/web/gui/src/main/webapp/');
        proxy.web(req, res);
      }
      else {
        return next();
      }
    }
  },
  'port': 3000,
  'open': false
};