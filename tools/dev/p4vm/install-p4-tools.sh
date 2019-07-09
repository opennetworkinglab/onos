#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Builds and installs all tools needed for developing and testing P4 support in
# ONOS.
#
# Tested on 16.04 and 18.04.
#
# Recommended minimum system requirements:
# 4 GB of RAM
# 2 cores
# 8 GB free hard drive space (~4 GB to build everything)
#
# To execute up to a given step, pass the step name as the first argument. For
# example, to install PI, but not bmv2, p4c, etc:
#   ./install-p4-tools.sh PI
# -----------------------------------------------------------------------------

# Exit on errors.
set -e
set -x

BMV2_COMMIT="10c2d3434a7212631f11d5d1e3bc802ba6365f6a"
PI_COMMIT="0bcaeda2269a4f2f0539cf8eac49868e389a8c18"
P4C_COMMIT="e2934ab32ace8a877bf2b34704950a4da69b6202"

# p4c seems to break when using protobuf versions newer than 3.2.0
PROTOBUF_VER=${PROTOBUF_VER:-3.2.0}
GRPC_VER=${GRPC_VER:-1.3.2}


BUILD_DIR=~/p4tools
NUM_CORES=`grep -c ^processor /proc/cpuinfo`
# If false, build tools without debug features to improve throughput of BMv2 and
# reduce CPU/memory footprint. Default is true.
DEBUG_FLAGS=${DEBUG_FLAGS:-true}
# Execute up to the given step (first argument), or all if not defined.
LAST_STEP=${1:-all}
# PI and BMv2 must be configured differently if we want to use Stratum
USE_STRATUM=${USE_STRATUM:-false}
# Improve time for one-time builds
FAST_BUILD=${FAST_BUILD:-false}
# Remove build artifacts
CLEAN_UP=${CLEAN_UP:-false}
BMV2_INSTALL=/usr/local
PI_INSTALL=/usr/local
set +x

function do_requirements {
    sudo apt update
    sudo apt-get install -y --no-install-recommends \
        autoconf \
        automake \
        bison \
        build-essential \
        cmake \
        cpp \
        curl \
        flex \
        git \
        graphviz \
        libavl-dev \
        libboost-dev \
        libboost-graph-dev \
        libboost-program-options-dev \
        libboost-system-dev \
        libboost-filesystem-dev \
        libboost-thread-dev \
        libboost-filesystem-dev \
        libboost-program-options-dev \
        libboost-system-dev \
        libboost-test-dev \
        libboost-thread-dev \
        libc6-dev \
        libev-dev \
        libevent-dev \
        libffi-dev \
        libfl-dev \
        libgc-dev \
        libgc1c2 \
        libgflags-dev \
        libgmp-dev \
        libgmp10 \
        libgmpxx4ldbl \
        libjudy-dev \
        libpcap-dev \
        libpcre3-dev \
        libssl-dev \
        libtool \
        make \
        pkg-config \
        python2.7 \
        python2.7-dev \
        tcpdump \
        wget \
        unzip

    sudo -H pip2.7 install setuptools cffi ipaddr ipaddress pypcap \
        git+https://github.com/p4lang/scapy-vxlan \
        git+https://github.com/p4lang/ptf.git
}

function do_requirements_1604 {
    sudo apt-get update
    sudo apt-get install -y --no-install-recommends \
        ca-certificates \
        g++ \
        libboost-iostreams1.58-dev \
        libreadline6 \
        libreadline6-dev \
        mktemp
}

function do_requirements_1804 {
    sudo apt-get update
    sudo apt-get install -y --no-install-recommends \
        ca-certificates \
        g++ \
        libboost1.65-dev \
        libboost-regex1.65-dev \
        libboost-iostreams1.65-dev \
        libreadline-dev \
        libssl1.0-dev
}

function do_protobuf {
    cd ${BUILD_DIR}
    if [[ ! -d protobuf-${PROTOBUF_VER} ]]; then
      # Get python package which also includes cpp.
      wget https://github.com/protocolbuffers/protobuf/releases/download/v${PROTOBUF_VER}/protobuf-python-${PROTOBUF_VER}.tar.gz
      tar -xzf protobuf-python-${PROTOBUF_VER}.tar.gz
      rm -r protobuf-python-${PROTOBUF_VER}.tar.gz
    fi
    cd protobuf-${PROTOBUF_VER}

    export CFLAGS="-Os"
    export CXXFLAGS="-Os"
    export LDFLAGS="-Wl,-s"
    ./autogen.sh
    confOpts="--prefix=/usr"
    if [[ "${FAST_BUILD}" = true ]] ; then
        confOpts="${confOpts} --disable-dependency-tracking"
    fi
    ./configure ${confOpts}
    make -j${NUM_CORES}
    sudo make install
    sudo ldconfig
    unset CFLAGS CXXFLAGS LDFLAGS

    cd python
    # Hack to get the -std=c++11 flag when building 3.6.1
    # https://github.com/protocolbuffers/protobuf/blob/v3.6.1/python/setup.py#L208
    export KOKORO_BUILD_NUMBER="hack"
    sudo -E python2.7 setup.py build --cpp_implementation
    sudo -E pip2.7 install .
    unset KOKORO_BUILD_NUMBER
}

function do_grpc {
    cd ${BUILD_DIR}
    if [[ ! -d grpc-${GRPC_VER} ]]; then
      git clone --depth 1 --single-branch --branch v${GRPC_VER} https://github.com/grpc/grpc.git grpc-${GRPC_VER}
    fi

    cd grpc-${GRPC_VER}
    git submodule update --init

    export LDFLAGS="-Wl,-s"
    RELEASE=`lsb_release -rs`
    if version_ge ${RELEASE} 18.04; then
       # Ubuntu 18.04 ships OpenSSL 1.1 by default, which has breaking changes in the API.
       # Here, we will build grpc with OpenSSL 1.0.
       # (Reference: https://github.com/grpc/grpc/issues/10589)
       # Also, set CFLAGS to avoid compilcation error caused by gcc7.
       # (Reference: https://github.com/grpc/grpc/issues/13854)
       PKG_CONFIG_PATH=/usr/lib/openssl-1.0/pkgconfig make -j${NUM_CORES} CFLAGS='-Wno-error'
    else
       make -j${NUM_CORES}
    fi
    sudo make install
    sudo ldconfig
    unset LDFLAGS

    sudo pip2.7 install -r requirements.txt
    sudo pip2.7 install .
}

function checkout_bmv2 {
    cd ${BUILD_DIR}
    if [[ ! -d bmv2 ]]; then
        git clone https://github.com/p4lang/behavioral-model.git bmv2
    fi
    cd bmv2
    git fetch
    git checkout ${BMV2_COMMIT}
}

function do_pi_bmv2_deps {
    checkout_bmv2
    # From bmv2's install_deps.sh.
    tmpdir=`mktemp -d -p .`
    cd ${tmpdir}
    if [[ "${USE_STRATUM}" = false ]] ; then
        bash ../travis/install-thrift.sh
    fi
    sudo ldconfig
    cd ..
    sudo rm -rf ${tmpdir}
}

function do_PI {
    cd ${BUILD_DIR}
    if [[ ! -d PI ]]; then
        git clone https://github.com/p4lang/PI.git
    fi
    cd PI
    git fetch
    git checkout ${PI_COMMIT}
    git submodule update --init --recursive

    ./autogen.sh
    if [[ "${USE_STRATUM}" = false ]] ; then
        ./configure --with-proto --without-internal-rpc --without-cli --prefix=${PI_INSTALL}
    else
        # Configure for Stratum
        ./configure --without-bmv2 --with-proto --with-fe-cpp --without-cli --without-internal-rpc --prefix=${PI_INSTALL}
    fi
    make -j${NUM_CORES}
    sudo make install
    sudo ldconfig
}

function do_bmv2 {
    checkout_bmv2

    ./autogen.sh

    confOpts="--with-pi --disable-elogger --without-nanomsg --without-targets"
    # We are building --without-targets but we know for sure we need those
    # parts of the BMv2 PI library for simple_switch.
    cppFlags="-I${PWD}/targets/simple_switch -DWITH_SIMPLE_SWITCH"
    if [[ "${FAST_BUILD}" = true ]] ; then
        confOpts="${confOpts} --disable-dependency-tracking"
    fi
    if [[ "${DEBUG_FLAGS}" = false ]] ; then
        confOpts="${confOpts} --disable-logging-macros"
    fi
    if [[ "${USE_STRATUM}" = true ]] ; then
        confOpts="--prefix=${BMV2_INSTALL} --without-thrift ${confOpts}"
        cppFlags="${cppFlags} -isystem${BMV2_INSTALL}/include -isystem${PI_INSTALL}/include -L${PI_INSTALL}/lib"
    fi
    confCmd="./configure CPPFLAGS=\"${cppFlags}\" ${confOpts}"
    eval ${confCmd}

    make -j${NUM_CORES}
    sudo make install
    cd targets/simple_switch
    make -j${NUM_CORES}
    sudo make install
    sudo ldconfig

    if [[ "${USE_STRATUM}" = false ]] ; then
        # Simple_switch_grpc target (not using Stratum)
        cd ../simple_switch_grpc
        ./autogen.sh
        ./configure --with-thrift
        make -j${NUM_CORES}
        sudo make install
        sudo ldconfig
    fi
}

function do_p4c {
    cd ${BUILD_DIR}
    if [[ ! -d p4c ]]; then
        git clone https://github.com/p4lang/p4c.git
    fi
    cd p4c
    git fetch
    git checkout ${P4C_COMMIT}
    git submodule update --init --recursive

    mkdir -p build
    cd build
    cmake .. -DENABLE_EBPF=OFF
    make -j${NUM_CORES}
    sudo make install
    sudo ldconfig
}

function check_commit {
    if [[ ! -e $2 ]]; then
        return 0 # true
    fi
    if [[ $(< $2) != "$1" ]]; then
        return 0 # true
    fi
    return 1 # false
}

# The following is borrowed from Mininet's util/install.sh
function version_ge {
    # sort -V sorts by *version number*
    latest=`printf "$1\n$2" | sort -V | tail -1`
    # If $1 is latest version, then $1 >= $2
    [[ "$1" == "$latest" ]]
}

function missing_lib {
    ldconfig -p | grep $1 &> /dev/null
    if [[ $? == 0 ]]; then
        echo "$1 found!"
        return 1 # false
    fi
    return 0 # true
}

function missing_protoc {
    command -v protoc >/dev/null 2>&1
    if [[ $? == 0 ]]; then
        protoc --version | grep $1 &> /dev/null
        if [[ $? == 0 ]]; then
            echo "protoc ${1} found!"
        else
            echo "A version of protoc was found, but not $1 (you may experience issues)"
        fi
        return 1 # false
    fi
    return 0 # true
}

function missing_grpc {
    # Is there a better way to check if a specific version of grpc is installed?
    if [[ -f /usr/local/lib/libgrpc++.so ]]; then
        ls -l /usr/local/lib/libgrpc++.so | grep libgrpc++.so.${1} &> /dev/null
        if [[ $? == 0 ]]; then
            echo "grpc ${1} found!"
        else
            echo "A version of grpc was found, but not $1 (you may experience issues)"
        fi
        return 1 # false
    else
        return 0 # true
    fi
}

function all_done {
    if [[ "${CLEAN_UP}" = true ]] ; then
        echo "Cleaning up build dir... ${BUILD_DIR})"
        sudo rm -rf ${BUILD_DIR}
    fi
    echo "Done!"
    exit 0
}

MUST_DO_ALL=false
DID_REQUIREMENTS=false
function check_and_do {
    # Check if the latest built commit is the same we are trying to build now,
    # or if all projects must be built. If true builds this project.
    commit_id="$1"
    proj_dir="$2"
    func_name="$3"
    step_name="$4"
    commit_file=${BUILD_DIR}/${proj_dir}/.last_built_commit_${step_name}
    if [[ ${MUST_DO_ALL} = true ]] \
        || check_commit ${commit_id} ${commit_file}; then
        echo "#"
        echo "# Building ${step_name} (${commit_id})"
        echo "#"
        # Print commands used to install to aid debugging
        set -x
        if ! ${DID_REQUIREMENTS} = true; then
            do_requirements
            # TODO consider other Linux distros; presently this script assumes
            # that it is running on Ubuntu.
            RELEASE=`lsb_release -rs`
            if version_ge ${RELEASE} 18.04; then
                do_requirements_1804
            elif version_ge ${RELEASE} 16.04; then
                do_requirements_1604
            else
                echo "Ubuntu version $RELEASE is not supported"
                exit 1
            fi
            DID_REQUIREMENTS=true
        fi
        eval ${func_name}
        if [[ -d ${BUILD_DIR}/${proj_dir} ]]; then
            # If project was built, we expect its dir. Otherwise, we assume
            # build was skipped.
            echo ${commit_id} > ${commit_file}
            # Build all next projects as they might depend on this one.
            MUST_DO_ALL=true
        fi
        # Disable printing to reduce output
        set +x
    else
        echo "${step_name} is up to date (commit ${commit_id})"
    fi
    # Exit if last step.
    if [[ ${step_name} = ${LAST_STEP} ]]; then
        all_done
    fi
}

mkdir -p ${BUILD_DIR}
cd ${BUILD_DIR}
# In dependency order.
if missing_protoc ${PROTOBUF_VER}; then
    check_and_do ${PROTOBUF_VER} protobuf-${PROTOBUF_VER} do_protobuf protobuf
fi
if missing_grpc ${GRPC_VER}; then
    check_and_do ${GRPC_VER} grpc-${GRPC_VER} do_grpc grpc
fi
check_and_do ${BMV2_COMMIT} bmv2 do_pi_bmv2_deps bmv2-deps
check_and_do ${PI_COMMIT} PI do_PI PI
check_and_do ${BMV2_COMMIT} bmv2 do_bmv2 bmv2
check_and_do ${P4C_COMMIT} p4c do_p4c p4c

all_done
