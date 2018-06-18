#!/usr/bin/env bash

# Installs Lubuntu desktop and code editors.
# Largely inspired by the P4.org tutorial VM scripts:
# https://github.com/p4lang/tutorials/

set -xe

sudo add-apt-repository ppa:webupd8team/sublime-text-3 -y
sudo add-apt-repository ppa:webupd8team/atom -y
sudo apt-get update

sudo DEBIAN_FRONTEND=noninteractive apt-get -y install wireshark
echo "wireshark-common wireshark-common/install-setuid boolean true" | sudo debconf-set-selections
sudo DEBIAN_FRONTEND=noninteractive dpkg-reconfigure wireshark-common

sudo apt-get -y --no-install-recommends install \
    lubuntu-desktop \
    atom \
    sublime-text-installer \
    vim \
    wget

# Disable screensaver
sudo apt-get -y remove light-locker

# Automatically log into the SDN user
cat << EOF | sudo tee -a /etc/lightdm/lightdm.conf.d/10-lightdm.conf
[SeatDefaults]
autologin-user=sdn
autologin-user-timeout=0
user-session=Lubuntu
EOF

# Vim
cd /home/sdn
mkdir -p .vim
mkdir -p .vim/ftdetect
mkdir -p .vim/syntax
echo "au BufRead,BufNewFile *.p4      set filetype=p4" >> .vim/ftdetect/p4.vim
echo "set bg=dark" >> .vimrc
wget https://github.com/p4lang/tutorials/blob/master/vm/p4.vim
mv p4.vim .vim/syntax/p4.vim

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

cat > ${DESKTOP}/Terminal << EOF
[Desktop Entry]
Encoding=UTF-8
Type=Application
Name=Terminal
Name[en_US]=Terminal
Icon=konsole
Exec=/usr/bin/x-terminal-emulator
Comment[en_US]=
EOF

cat > ${DESKTOP}/Wireshark << EOF
[Desktop Entry]
Encoding=UTF-8
Type=Application
Name=Wireshark
Name[en_US]=Wireshark
Icon=wireshark
Exec=/usr/bin/wireshark
Comment[en_US]=
EOF

cat > ${DESKTOP}/Sublime\ Text << EOF
[Desktop Entry]
Encoding=UTF-8
Type=Application
Name=Sublime Text
Name[en_US]=Sublime Text
Icon=sublime-text
Exec=/opt/sublime_text/sublime_text
Comment[en_US]=
EOF

cat > ${DESKTOP}/Atom << EOF
[Desktop Entry]
Encoding=UTF-8
Type=Application
Name=Atom
Name[en_US]=Atom
Icon=atom
Exec=/usr/bin/atom
Comment[en_US]=
EOF
