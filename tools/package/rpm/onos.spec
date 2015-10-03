Name:        onos
Version:     @ONOS_RPM_VERSION
Release:     1
Summary:     Open Networking Operating System (ONOS)
Vendor:      ONOS Project
Packager:    ONOS Project

Group:       Applications/Engineering
License:     Apache 2.0
Requires:    jre >= 1:8
URL:         http://www.onosproject.org
Source0:     %{name}-@ONOS_RPM_VERSION.tar.gz

BuildArch: noarch
BuildRoot: %{_tmppath}/%{name}-buildroot

%description
Open Network Operating System (ONOS) is an open source SDN controller.

%prep
%setup -q

%install
mkdir -p %{buildroot}
cp -R * %{buildroot}

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
/etc/init/onos.conf
/opt/onos/

%post
ONOS_USER=sdn

# Check to see if user exists, and if not, create a service account
getent passwd $ONOS_USER >/dev/null 2&>1 || ( useradd -M $ONOS_USER && usermod -L $ONOS_USER )

# Create ONOS options file
[ ! -f /opt/onos/options ] && cat << EOF > /opt/onos/options
export ONOS_OPTS=server
export ONOS_USER="$ONOS_USER"
EOF

# Change permissions for onos directory
[ -d /opt/onos ] && chown -R $ONOS_USER.$ONOS_USER /opt/onos

echo ONOS successfully installed at /opt/onos

%preun
# Check if onos is running; if it is, stop onos
[ -z "$(status onos 2>/dev/null | grep start)" ] && echo "onos is not running." || (
    stop onos

    # Wait for onos to stop up to 5 seconds
    for i in $(seq 1 5); do
      [ -z "$(ps -ef | grep karaf.jar | grep -v grep)" ] && break
      sleep 1
    done
    [ -z "$(ps -ef | grep karaf.jar | grep -v grep)" ] && echo 'Stopped onos service' || echo 'Failed to stop onos'
)

%postun
#TODO this should be less brute-force
rm -rf /opt/onos

%changelog
# TODO

