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
        setenv JAVA_HOME `/usr/libexec/java_home -v 1.8`
    else if ( -d /usr/lib/jvm/java-8-oracle ) then
        setenv JAVA_HOME /usr/lib/jvm/java-8-oracle
    else if ( -d /usr/lib/jvm/java-8-openjdk-amd64 ) then
        setenv JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64
    endif
endif
if ( ! $?MAVEN ) then
    setenv MAVEN $HOME/Applications/apache-maven-3.3.9
endif
if ( ! $?KARAF_VERSION ) then
    setenv KARAF_VERSION 3.0.8
endif
if ( ! $?KARAF_ROOT ) then
    setenv KARAF_ROOT $HOME/Applications/apache-karaf-$KARAF_VERSION
endif
setenv KARAF_LOG $KARAF_ROOT/data/log/karaf.log

alias onos-setup-cell ' ( env ONOS_CELL=\!^ $ONOS_ROOT/tools/test/bin/onos-show-cell \!^ ) && setenv ONOS_CELL \!^'

set path=( $path $ONOS_ROOT/tools/dev/bin $ONOS_ROOT/tools/test/bin )
set path=( $path $ONOS_ROOT/tools/build )
set path=( $path $KARAF_ROOT/bin )
