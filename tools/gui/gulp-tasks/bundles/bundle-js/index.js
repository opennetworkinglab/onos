import gulp from 'gulp';
import concat from 'gulp-concat';
import strip from 'gulp-strip-comments';
import uglyfy from 'gulp-uglify';
import sourceMaps from 'gulp-sourcemaps';
import BundleResources from '../helpers/bundleResources';
import { reload } from '../../dev-server';

const GUI_BASE = '../../web/gui/src/main/webapp/';
const bundleFiles = [
     // NOTE: Bundle the important files first
    'app/directives.js',
    'app/fw/util/util.js',
    'app/fw/mast/mast.js',
    'app/fw/nav/nav.js',
    'app/fw/svg/svg.js',
    'app/fw/remote/remote.js',
    'app/fw/widget/widget.js',
    'app/fw/layer/layer.js',

    // NOTE: bundle everything else
    'app/fw/**/*.js',
    'app/view/**/*.js'
];

const vendor = [
    'tp/angular.js',
    'tp/angular-route.js',
    'tp/angular-cookies.js',
    'tp/d3.js',
    'tp/topojson.v1.min.js',
    'tp/Chart.js',
    'tp/lodash.min.js',
];

function bundle(files, exportName) {

    // TODO: Use util.noop once npm issues have been resolved
    if (process.argv.indexOf('--development') > -1) {
        return gulp.src(BundleResources(GUI_BASE, files))
            .pipe(sourceMaps.init())
            .on('error', (e, file, line) => console.error(e))
            .pipe(concat(exportName))
            .pipe(sourceMaps.write('source-map'))
            .pipe(gulp.dest(GUI_BASE + '/dist/'))
            .on('end', () => { reload(); })
    }

    return gulp.src(BundleResources(GUI_BASE, files))
        .pipe(sourceMaps.init())
        .pipe(strip())
        .pipe(uglyfy())
        .on('error', (e, file, line) => console.error(e))
        .pipe(concat(exportName))
        .pipe(sourceMaps.write('source-map'))
        .pipe(gulp.dest(GUI_BASE + '/dist/'));
}

const tasks = function () {
    // gulp.task('bundle-vendor', () => bundle(vendor, 'vendor.js'));
    gulp.task('bundle-js', () => bundle(bundleFiles, 'onos.js'));
    gulp.task('watch-js', () => {
        gulp.watch([GUI_BASE + 'app/**/*.js'], ['bundle-js']);
    }).on('change', (event) => {
        console.log('File ' + event.path + ' was ' + event.type + ', running tasks...');
    });
};

export default tasks();