#!/usr/bin/env bash

# Installs desktop utilities and code editors.
# Largely inspired by the P4.org tutorial VM scripts:
# https://github.com/p4lang/tutorials/

set -xe

# Remove unneeded software
sudo apt-get remove -y --purge \
    libreoffice* \
    account-plugin-aim \
    account-plugin-facebook \
    account-plugin-flickr \
    account-plugin-jabber \
    account-plugin-salut \
    account-plugin-yahoo \
    aisleriot \
    gnome-mahjongg \
    gnome-mines \
    gnome-sudoku \
    landscape-client-ui-install \
    unity-lens-music \
    unity-lens-photos \
    unity-lens-video \
    unity-scope-audacious \
    unity-scope-chromiumbookmarks \
    unity-scope-clementine \
    unity-scope-colourlovers \
    unity-scope-devhelp \
    unity-scope-firefoxbookmarks \
    unity-scope-gmusicbrowser \
    unity-scope-gourmet \
    unity-scope-musicstores \
    unity-scope-musique \
    unity-scope-openclipart \
    unity-scope-texdoc \
    unity-scope-tomboy \
    unity-scope-video-remote \
    unity-scope-virtualbox \
    unity-scope-zotero \
    unity-webapps-common

sudo add-apt-repository ppa:webupd8team/sublime-text-3 -y
sudo add-apt-repository ppa:webupd8team/atom -y
sudo apt-get update

sudo DEBIAN_FRONTEND=noninteractive apt-get -y install wireshark
echo "wireshark-common wireshark-common/install-setuid boolean true" | sudo debconf-set-selections
sudo DEBIAN_FRONTEND=noninteractive dpkg-reconfigure wireshark-common

sudo apt-get -y --no-install-recommends install \
    atom \
    sublime-text-installer \
    vim

# TODO: Disable screensaver and automatically log into the SDN user

# Sublime
cd /home/sdn
mkdir -p ~/.config/sublime-text-3/Packages/
cd .config/sublime-text-3/Packages/
git clone https://github.com/c3m3gyanesh/p4-syntax-highlighter.git

# Atom
apm install language-p4

# Adding Desktop icons
DESKTOP=/home/sdn/Desktop
mkdir -p ${DESKTOP}

cat > ${DESKTOP}/Wireshark.desktop << EOF
[Desktop Entry]
Encoding=UTF-8
Type=Application
Name=Wireshark
Name[en_US]=Wireshark
Icon=wireshark
Exec=/usr/bin/wireshark
Comment[en_US]=
EOF

cat > ${DESKTOP}/Sublime\ Text.desktop << EOF
[Desktop Entry]
Encoding=UTF-8
Type=Application
Name=Sublime Text
Name[en_US]=Sublime Text
Icon=sublime-text
Exec=/opt/sublime_text/sublime_text
Comment[en_US]=
EOF

cat > ${DESKTOP}/Atom.desktop << EOF
[Desktop Entry]
Encoding=UTF-8
Type=Application
Name=Atom
Name[en_US]=Atom
Icon=atom
Exec=/usr/bin/atom
Comment[en_US]=
EOF

chmod +x ${DESKTOP}/*.desktop
