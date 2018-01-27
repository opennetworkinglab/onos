#!/usr/bin/env bash

YANG_ROOT=$1

CONFDC_ARGS=' -c'

# YANGPATHS
for path in $(find $YANG_ROOT -type d); do
  CONFDC_ARGS+=" --yangpath $path"
done

# create output dir
mkdir -p fxs

# compile .yang s
for yang in $(find $YANG_ROOT -type f -name '*.yang'); do
  BASE=$(basename $yang)
  OUT="${BASE%.yang}.fxs"
  echo "Compiling..$yang"
  confdc $CONFDC_ARGS -o fxs/$OUT -- $yang
done
