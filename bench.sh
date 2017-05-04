#!/bin/bash

# The HikariCP test case extends the base yaml with the following
# additional configuration
HIKARI_YAML=$(cat <<'EOF'
hikari:
  datasourceClassName: org.postgresql.ds.PGSimpleDataSource
  properties:
    'databaseName': 'postgres'
  user: nick
  minSize: ${poolSize}
  maxSize: ${poolSize}
  password: nick
EOF
)

# The Tomcat test case extends the base yaml with the following
# additional configuration. Notice how more configuration options
# are specified, one of the selling points of Hikari is that
# there are no "unsafe" options.
TOMCAT_YAML=$(cat <<'EOF'
tomcat:
  driverClass: org.postgresql.Driver
  url: jdbc:postgresql://localhost/postgres
  user: nick
  minSize: ${poolSize}
  maxSize: ${poolSize}
  initialSize: ${poolSize}
  rollbackOnReturn: true
  checkConnectionOnBorrow: true
  autoCommitByDefault: false
  validationInterval: '1 second'
  validatorClassName: 'com.example.TomValidator'
  jdbcInterceptors: "ConnectionState;StatementFinalizer"
  password: nick
EOF
)

# The actual function to do the load test
load_test () {
    YAML="$1"
    CONNECTIONS="$3"
    URL="http://benchmark:8080?user=87"

    # Nested for loop creates 42 configurations for each
    # test case, which causes this to be long script!
    for i in 1 2 4 8 16 32; do
    for j in 1 2 4 8 16 32 64; do
        export POOL_SIZE=$i
        export MAX_THREADS=$((6 + j))
        export CONFIG=$2

        # A pool size that is bigger than the number of max requests doesn't
        # really make sense because then there will always be some connections
        # not being used, so skip the iteration in the interest in time
        if [[ "$POOL_SIZE" -gt "$MAX_THREADS" ]]; then
            continue
        fi

        echo "Pool: ${POOL_SIZE}. Server threads: ${MAX_THREADS}"
        echo "${YAML}" | ssh benchmark "cat - config_base.yaml > config_new.yaml
            # Kill the previous server instance if one exists
            pkill java

            # Wait for it to cleanly shut down as we reuse ports, etc
            sleep 3s

            # Set the environment variables used in the configuration files
            # and use nohup so that when this ssh command (and connection) exits
            # the server keeps running
            poolSize=${POOL_SIZE} maxThreads=${MAX_THREADS} nohup \
                java -jar bench-1.0-SNAPSHOT.jar server config_new.yaml >/dev/null 2>&1 &

            # Wait for the server to properly initialize
            sleep 5s"

        # Load test for 60 seconds with four threads (as this machine has four
        # CPUs to dedicate to load testing) Also use a custom lua script that
        # reports various statistics into a csv format for further analysis as
        # there'll be 80+ rows, with each row having several statistics.
        # We're using "tee" here so that we can see all the stdout but only
        # the last line, which is what is important to the csv is appended
        # to the csv
        wrk -c "$CONNECTIONS" -d 60s -t 4 -s report.lua ${URL} | \
            tee >(tail -n 1 >> wrk-$CONNECTIONS.csv)
    done;
    done;
}

# Call our function!
load_test "$TOMCAT_YAML" "tomcat" 300
load_test "$HIKARI_YAML" "hikari" 300
