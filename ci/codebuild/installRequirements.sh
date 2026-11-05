#!/bin/bash

DIR=`pwd`

yum install -y ghostscript
yum install -y \
  libwpd-tools \
  libpng-devel \
  libjpeg-devel \
  libtiff-devel \
  perl \
  perl-Archive-Zip \
  poppler-utils \
  tar \
  unzip \
  wget \
  gzip \
  xz \
  aspell \
  google-noto-cjk-fonts


export LIBREOFFICE_VERSION=7.5.8

curl -f -L https://packages.nuxeo.com/repository/document-foundation-raw/LibreOffice_${LIBREOFFICE_VERSION}_Linux_x86-64_rpm.tar.gz | tar -C /tmp -xzv
yum -y localinstall /tmp/LibreOffice_${LIBREOFFICE_VERSION}*/RPMS/*.rpm
ln -s /opt/libreoffice$(echo $LIBREOFFICE_VERSION | cut -f 1,2 -d ".")/program/soffice /usr/bin/soffice
rm -rf /tmp/LibreOffice_${LIBREOFFICE_VERSION}*

mv /usr/local/bin/docker /usr/bin/docker
#curl -L https://github.com/docker/compose/releases/download/v2.11.2/docker-compose-linux-x86_64 /usr/bin/docker-compose
#sudo chmod 755 /usr/bin/docker-compose


curl -f -L https://johnvansickle.com/ffmpeg/old-releases/ffmpeg-5.1.1-amd64-static.tar.xz | tar -C /opt -xJv
export PATH="$PATH:/opt/ffmpeg-5.1.1-amd64-static/"

# ImageMagick 7 from source, as on release/2026.3: yum's ImageMagick 6 lacks the image delegates the picture tests need.
curl -f -L https://github.com/ImageMagick/ImageMagick/archive/refs/tags/7.1.2-25.tar.gz  | tar -C /opt -xz
cd  /opt/ImageMagick-7.1.2-25
./configure --without-x
sudo make && sudo make install

identify -version

# ExifTool from CPAN: SourceForge and exiftool.org drop older versions (13.45 now gives a 404), CPAN keeps them.
# A failed install stops the build here, not an hour later in the binary-metadata tests.
EXIFTOOL_VERSION=13.55
curl -f -L https://cpan.metacpan.org/authors/id/E/EX/EXIFTOOL/Image-ExifTool-${EXIFTOOL_VERSION}.tar.gz | tar -C /opt -xz \
  && cd /opt/Image-ExifTool-${EXIFTOOL_VERSION}/ \
  && perl Makefile.PL \
  && sudo make install \
  && exiftool -ver \
  || { echo "ExifTool ${EXIFTOOL_VERSION} did not install" >&2; return 1 2>/dev/null || exit 1; }

docker-compose --version

cd $DIR