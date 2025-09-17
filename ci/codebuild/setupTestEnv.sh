#!/bin/bash
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

docker-compose --file $SCRIPT_DIR/docker-compose.yaml up -d

#echo "nuxeo.test.opensearch1.servers=http://localhost:9200" >> ~/nuxeo-test-opensearch.properties
#echo "nuxeo.test.search=opensearch1" >> ~/nuxeo-test-opensearch.properties
echo "nuxeo.test.mongodb.dbname=nuxeo" >> ~/nuxeo-test-mongodb.properties
echo "nuxeo.test.mongodb.server=mongodb://localhost" >> ~/nuxeo-test-mongodb.properties
echo "nuxeo.test.s3storage.bucket=nuxeo-platform-unit-tests" >> ~/nuxeo-test-mongodb.properties
echo "nuxeo.test.s3storage.transient.bucket=nuxeo-platform-unit-tests-transient" >> ~/nuxeo-test-mongodb.properties
echo "nuxeo.test.s3storage.policy.bucket=nuxeo-platform-unit-tests-policy" >> ~/nuxeo-test-mongodb.properties
echo "nuxeo.test.s3storage.bucket_prefix=test-06102022" >> ~/nuxeo-test-mongodb.properties
echo "nuxeo.test.s3storage.provider.test.bucket_prefix=test-06102022-test" >> ~/nuxeo-test-mongodb.properties
echo "nuxeo.test.s3storage.provider.other.bucket_prefix=test-06102022-other" >> ~/nuxeo-test-mongodb.properties
