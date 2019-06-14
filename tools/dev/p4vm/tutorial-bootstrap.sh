#!/usr/bin/env bash

set -xe

# Desktop and other tutorial tools.
# VirtualBox doesn't like Gnome, use Unity:
# https://askubuntu.com/questions/1035410/ubuntu-18-04-gnome-hangs-on-virtualbox-with-3d-acceleration-enabled
echo "gdm3 shared/default-x-display-manager select lightdm" | debconf-set-selections
echo "lightdm shared/default-x-display-manager select lightdm" | debconf-set-selections

# Install ubuntu desktop from tasksel
apt-get install -y --no-install-recommends tasksel
DEBIAN_FRONTEND=noninteractive tasksel install ubuntu-desktop
# Remove gnome, install unity
apt-get remove -y gdm3 ubuntu-desktop
DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    ubuntu-unity-desktop lightdm \
    gnome-panel \
    gnome-settings-daemon \
    metacity \
    nautilus

# FIXME: app menu is empty in unity

snap install intellij-idea-community --classic
# TODO: install plugins, P4 plugin and Python CE

DEBIAN_FRONTEND=noninteractive apt-get -y install wireshark
echo "wireshark-common wireshark-common/install-setuid boolean true" | debconf-set-selections
DEBIAN_FRONTEND=noninteractive dpkg-reconfigure wireshark-common
