#!/bin/sh

set -x

docker-compose build --no-cache
docker-compose up
