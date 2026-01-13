mvn versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false $MAVEN_ARGS
sed -i -E "s/<nuxeo.platform.version>.*?<\/nuxeo.platform.version>/<nuxeo.platform.version>$VERSION<\/nuxeo.platform.version>/" pom.xml
sed -i -E "s/<version>\\$\{nuxeo.platform.version\}<\/version>/<version>$VERSION<\/version>/" pom.xml
sed -i -E "s/org.nuxeo.ecm.product.version=.*/org.nuxeo.ecm.product.version=$VERSION/" server/nuxeo-nxr-server/src/main/resources/templates/nuxeo.defaults
sed -i -E "s/<version>.*?<\/version>/<version>$VERSION<\/version>/" parent/pom.xml
sed -i -E "s/<version>.*?<\/version>/<version>$VERSION<\/version>/" ci/release/pom.xml