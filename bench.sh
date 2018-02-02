#!/bin/bash

set -euo pipefail

function clean_up {
    echo "Cleaning up"
    docker-compose down
}

trap clean_up SIGHUP SIGINT SIGTERM

docker-compose up -d db
docker-compose build web

# The actual function to do the load test
function load_test {
    # Nested for loop creates 42 configurations for each
    # test case, which causes this to be long script!
    for i in 1 2 4 8 16 32; do
    for j in 1 2 4 8 16 32 64; do
        export POOL_SIZE=$i
        export MAX_THREADS=$((6 + j))

        # A pool size that is bigger than the number of max requests doesn't
        # really make sense because then there will always be some connections
        # not being used, so skip the iteration in the interest in time
        if [[ "$POOL_SIZE" -gt "$MAX_THREADS" ]]; then
            continue
        fi

        load_test_inner "tomcat-config.yaml" "tomcat"
        load_test_inner "hikari-config.yaml" "hikaricp"
    done;
    done;
}

function load_test_inner {
    YAML="$1"
    export CONFIG="$2"
    URL="http://127.0.0.1:8080?user=87"
    echo "Config: ${CONFIG}. Pool: ${POOL_SIZE}. Server threads: ${MAX_THREADS}"
    NAME=$(docker-compose run -d -e POOL_SIZE=$POOL_SIZE -e MAX_THREADS=$MAX_THREADS --service-ports web "$YAML")
    sleep 2

    # Warmup
    wrk -c 100 -d 60s -t 4 ${URL}

    # Load test for 10 seconds with four threads (as this machine has four
    # CPUs to dedicate to load testing). Also use a custom lua script that
    # reports various statistics into a csv format for further analysis as
    # there'll be 80+ rows, with each row having several statistics.
    # We're using "tee" here so that we can see all the stdout but only
    # the last line, which is what is important to the csv is appended
    # to the csv
    for k in {0..4}; do
        wrk -c 100 -d 10s -t 4 -s report.lua ${URL} | \
            tee >(tail -n 1 >> wrk-100.csv)
    done;

    docker stop $NAME
    docker rm $NAME
}

load_test

clean_up
