#!/bin/bash

yum install -y perl-Image-ExifTool
yum install -y ghostscript
yum install -y exifTool \
  ImageMagick \
  libwpd-tools \
  perl-Archive-Zip \
  poppler-utils \
  tar \
  ufraw \
  unzip \
  wget \
  google-noto-cjk-fonts


export LIBREOFFICE_VERSION=7.1.5

curl -f -L https://packages.nuxeo.com/repository/document-foundation-raw/LibreOffice_${LIBREOFFICE_VERSION}_Linux_x86-64_rpm.tar.gz | tar -C /tmp -xzv
yum -y localinstall /tmp/LibreOffice_${LIBREOFFICE_VERSION}*/RPMS/*.rpm
ln -s /opt/libreoffice$(echo $LIBREOFFICE_VERSION | cut -f 1,2 -d ".")/program/soffice /usr/bin/soffice
rm -rf /tmp/LibreOffice_${LIBREOFFICE_VERSION}*

mv /usr/local/bin/docker /usr/bin/docker
#curl -L https://github.com/docker/compose/releases/download/v2.11.2/docker-compose-linux-x86_64 /usr/bin/docker-compose
#sudo chmod 755 /usr/bin/docker-compose


curl -f -L https://johnvansickle.com/ffmpeg/old-releases/ffmpeg-5.1.1-amd64-static.tar.xz | tar -C /opt -xJv

export PATH="$PATH:/opt/ffmpeg-5.1.1-amd64-static/"

docker-compose --version
