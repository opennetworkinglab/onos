import gulp from 'gulp';
import * as Tasks from './gulp-tasks/';

gulp.task('build', ['bower', 'bundle-css', 'bundle-js']);
gulp.task('tests', ['bower', 'test']);
gulp.task('default', ['bundle-js', 'bundle-css', 'serve', 'watch-js', 'watch-css']);