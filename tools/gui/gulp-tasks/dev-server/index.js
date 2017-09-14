import gulp from 'gulp';
import browserSync from 'browser-sync';
import fs from 'fs';
import webserver from 'gulp-webserver';
import proxy from 'http-proxy-middleware';

let external_apps;
let bs = null;

const files = ['../../web/gui/src/main/webapp/**/*.js'];
const defaultViews = fs.readdirSync('../../web/gui/src/main/webapp/app/view/');
const viewNameMatcher = new RegExp(/\/onos\/ui\/app\/view\/(.+)\/.+\.(?:js|css|html)/);

if (process.env.ONOS_EXTERNAL_APP_DIRS) {
    let external_apps = process.env.ONOS_EXTERNAL_APP_DIRS.replace(/\s/,'').split(',');

    external_apps = external_apps.reduce(function (dict, app) {
        const pieces = app.split(':');
        const appName = pieces[0];
        const appPath = pieces[1];
        dict[appName] = appPath;
        return dict;
    }, {});
}

const checkExternalApp = (url) => {
    if(external_apps){
        for(let i = 0; i < Object.keys(external_apps).length; i++){
            const key = Object.keys(external_apps)[i];
            if (url.indexOf(key) !== -1) {
                return key;
            }
        }
    }
    return false;
};

const serve = () => {
    bs = browserSync.init({
        proxy: {
            target: 'http://localhost:8181',
            ws: true,
            middleware: [
                proxy(['**/*.js', '!/onos/ui/onos.js'], { target: 'http://localhost:8189' }),
                proxy(['**/*.css'], { target: 'http://localhost:8189' }),
                proxy('**/*.js.map', {
                    target: 'http://localhost:8189',
                    changeOrigin: true,
                    logLevel: 'debug'
                })
            ]
        }
    });
};

const tasks = () => {
    gulp.task('serve', ['bundle-js', 'proxy-server'], serve);
    gulp.task('proxy-server', function () {
        gulp.src('../../web/gui/src/main/webapp')
            .pipe(webserver({
                port: 8189,
                path: '/onos/ui/'
            }));
    });
};

export default tasks();

export function reload() {
    if (bs) {
        bs.reload();
    }
}

