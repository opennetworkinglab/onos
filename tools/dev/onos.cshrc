#!/bin/tcsh
# ONOS developer csh/tcsh profile conveniences
# Simply include in your own $HOME/.cshrc file. E.g.:
#
#    setenv ONOS_ROOT $HOME/onos
#    if ( -f $ONOS_ROOT/tools/dev/onos.cshrc ) then
#        source $ONOS_ROOT/tools/dev/onos.cshrc
#    endif
#     

# Root of the ONOS source tree
if ( ! $?ONOS_ROOT ) then
    setenv ONOS_ROOT $HOME/onos
endif

# Setup some environmental context for developers
if ( ! $?JAVA_HOME ) then
    if ( -x /usr/libexec/java_home ) then
        setenv JAVA_HOME `/usr/libexec/java_home -v 1.7`
    else if ( -d /usr/lib/jvm/java-7-openjdk-amd64 ) then
        setenv JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64
    endif
endif
if ( ! $?MAVEN ) then
    setenv MAVEN $HOME/Applications/apache-maven-3.2.2
endif
if ( ! $?KARAF ) then
    setenv KARAF $HOME/Applications/apache-karaf-3.0.1
endif
setenv KARAF_LOG $KARAF/data/log/karaf.log

alias onos-setup-cell ' ( env ONOS_CELL=\!^ $ONOS_ROOT/tools/test/bin/onos-show-cell \!^ ) && setenv ONOS_CELL \!^'

set path=( $path $ONOS_ROOT/tools/dev/bin $ONOS_ROOT/tools/test/bin )
set path=( $path $ONOS_ROOT/tools/build )
set path=( $path $KARAF/bin )
