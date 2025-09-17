#!/bin/bash

aws --profile sandbox ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin 396897641652.dkr.ecr.eu-west-1.amazonaws.com

docker tag nuxeo/nuxeo:latest 396897641652.dkr.ecr.eu-west-1.amazonaws.com/nuxeo-sb:latest
docker push 396897641652.dkr.ecr.eu-west-1.amazonaws.com/nuxeo-sb:latest


