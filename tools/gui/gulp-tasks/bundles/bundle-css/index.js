import gulp from 'gulp';
import concat from 'gulp-concat';
import BundleResources from '../helpers/bundleResources';

const GUI_BASE = '../../web/gui/src/main/webapp/';
const bundleFiles = [
    'app/onos.css',
    'app/onos-theme.css',
    'app/common.css',
    'app/fw/**/*.css',
    'app/view/**/*.css',
];

const task = gulp.task('bundle-css', function () {
    return gulp.src(BundleResources(GUI_BASE, bundleFiles))
        .pipe(concat('onos.css'))
        .pipe(gulp.dest(GUI_BASE + '/dist/'));
});

export default task;