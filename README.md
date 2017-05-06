# Dropwizard HikariCP Benchmark

By default, [Dropwizard](https://github.com/dropwizard/dropwizard) bundles [Tomcat JDBC](https://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html) for database [connection pooling](https://en.wikipedia.org/wiki/Connection_pool). However, Tomcat JDBC isn't the only competition, [HikariCP](https://github.com/brettwooldridge/HikariCP) exists (among others), and claims safety and performance. This repo is to create a reproducible testbed for comparing these two connection pools. Realistically, one may not see a difference in performance between Tomcat and HikariCP in their applications, as even a 50% speed improvement won't mean much if only accounts for 5% of the costs, but each pool brings other tangible benefits to the table.

This repo uses a yet released version of Dropwizard (1.2.0), so one will need to install the latest master branch.

## Data

The data used in this experiment is the [dataset (200MB gzip link)](https://github.com/dgrtwo/StackLite/blob/84136173ad5982c7d6cb6bffe3afc9b389c0dc47/questions.csv.gz) from a ["A simple dataset of Stack Overflow questions and tags"](https://github.com/dgrtwo/StackLite).

Below is a snippet of the data using the excellent [xsv tool](https://github.com/BurntSushi/xsv):

```
$ gzip -d -c questions.csv.gz | xsv slice --end 10 | xsv table

Id  CreationDate          ClosedDate            DeletionDate          Score  OwnerUserId  AnswerCount
1   2008-07-31T21:26:37Z  NA                    2011-03-28T00:53:47Z  1      NA           0
4   2008-07-31T21:42:52Z  NA                    NA                    458    8            13
6   2008-07-31T22:08:08Z  NA                    NA                    207    9            5
8   2008-07-31T23:33:19Z  2013-06-03T04:00:25Z  2015-02-11T08:26:40Z  42     NA           8
9   2008-07-31T23:40:59Z  NA                    NA                    1410   1            58
11  2008-07-31T23:55:37Z  NA                    NA                    1129   1            33
13  2008-08-01T00:42:38Z  NA                    NA                    451    9            25
14  2008-08-01T00:59:11Z  NA                    NA                    290    11           8
16  2008-08-01T04:59:33Z  NA                    NA                    78     2            5
17  2008-08-01T05:09:55Z  NA                    NA                    114    2            11
```

## PostgreSQL

Our database of choice will be [Postgres](https://www.postgresql.org/), but we'll need to configure the box to aid performance. [PostgreSQL 9.0 High Performance](https://www.amazon.com/PostgreSQL-High-Performance-Gregory-Smith/dp/184951030X) comes chock-full of performance tips. The following tips were applied from the book:

- Set the disk read ahead to 4096: blockdev --setra 4096 /dev/sda
- Prevent the OS from updating file times by mounting the filesystem with `noatime`
- vm.swappiness=0
- vm.overcommit_memory=2

Caveat, it is most likely that none of these tweaks will have a significant
impact because query will be against a single user (so Postgres won't have to
go far to fetch the data from its cache).

The following Postgres configurations were taken from
[PGTune](http://pgtune.leopard.in.ua/) for a 4GB web application.

```
max_connections = 200
shared_buffers = 1GB
effective_cache_size = 3GB
work_mem = 5242kB
maintenance_work_mem = 256MB
min_wal_size = 1GB
max_wal_size = 2GB
checkpoint_completion_target = 0.7
wal_buffers = 16MB
default_statistics_target = 10
```

## The SQL

There'll only be one table for all the data. We'll first load the data using
Postgres's awesome
[`COPY`](https://www.postgresql.org/docs/9.5/static/sql-copy.html) command (the
data should be uncompressed first unless using the `PROGRAM` directive)

```sql
CREATE TABLE questions (
    id serial PRIMARY KEY,
    creationDate TIMESTAMPTZ,
    closedDate TIMESTAMPTZ,
    deletionDate TIMESTAMPTZ,
    score int,
    ownerUserId int,
    answerCount int
);

COPY questions FROM '/tmp/questions.csv'
WITH (HEADER, FORMAT CSV, NULL 'NA');

CREATE INDEX user_idx ON questions(ownerUserId);
```

Notice the index on the user id was created at the end -- for performance reasons.

## Running the Benchmark

- Create two machines, one to host the application and the other to host the load tester
- On the application server:
  - Install postgres
  - Add a new user `nick` with password `nick`
  - Load up the data
  - Install Java 8
  - Copy over the built benchmark jar and config_base.yaml
- On the load testing server:
  - Install wrk
  - Copy over `bench.sh` and `report.lua`
  - Install ssh key to the application server.

## Results

Work in progress :smile:

The environment in which the database and the server were deployed:

- 4 cores of an i7-6700k
- 8GB RAM
- Ubuntu 14.04
- Postgres 9.5

Most apps will find this a deficient setup for their production environment, but this is what I have, so I encourage those to run the benchmark on their own servers.

To start the analysis off, let's look at the top 5 configurations for both HikariCP and Tomcat and see their response latencies and request throughtput

![](https://github.com/nickbabcock/dropwizard-hikaricp-benchmark/raw/master/img/top-response-latencies.png)

![](https://github.com/nickbabcock/dropwizard-hikaricp-benchmark/raw/master/img/top-response-throughput.png)

HikariCP and Tomcat have similar latencies when included in a Dropwizard application, but it appears that HikariCP has an edge when it comes to throughput; however, I don't believe it is significant enough. If we look at the same graphs but across all configurations (i.e. including *sub-optimal* configurations) the story is a little different.

![](https://github.com/nickbabcock/dropwizard-hikaricp-benchmark/raw/master/img/response-latencies.png)

![](https://github.com/nickbabcock/dropwizard-hikaricp-benchmark/raw/master/img/response-throughput.png)

The data is a lot more mixed. This is testament that it is important to for one to configure their server and db pools. So while HikariCP is faster across microbenchmarks it may not make a large difference in an application. A game of inches sometimes doesn't matter when a request has to go a mile.

The top five configurations for HikariCP for the environment:

|config | pool size| max threads| requests|  mean|  stdev|   p50|    p90|    p99|
|:------|---------:|-----------:|--------:|-----:|------:|-----:|------:|------:|
|hikari |         8|           8|   975405| 7.123|  9.886| 5.429| 10.349| 32.639|
|hikari |         4|           4|   870198| 7.652|  8.659| 6.378| 10.660| 32.423|
|hikari |        16|          16|   869029| 7.972|  8.705| 5.925| 13.409| 36.106|
|hikari |        16|          32|   805108| 8.646|  9.624| 6.712| 14.248| 42.048|
|hikari |        32|          32|   779251| 9.358| 10.504| 6.537| 17.808| 48.531|

The top five configurations for Tomcat for the environment:

|config | pool size| max threads| requests|  mean|  stdev|   p50|    p90|    p99|
|:------|---------:|-----------:|--------:|-----:|------:|-----:|------:|------:|
|tomcat |        16|          16|   876972| 7.648|  9.525| 6.545| 11.051| 29.946|
|tomcat |         8|           8|   856844| 7.766|  9.427| 6.763| 11.014| 30.708|
|tomcat |         8|          16|   796295| 8.391|  9.903| 7.256| 11.398| 32.154|
|tomcat |        16|          32|   773777| 9.120| 11.611| 7.041| 13.791| 45.019|
|tomcat |        32|          32|   769750| 9.110| 11.270| 7.136| 14.840| 43.041|

So what are the defaults for Dropwizard?:

 - Min pool size: 10
 - Max pool size: 100
 - Min request threads: ~1
 - Max request threads: ~1024

Whoa, unless you're running incredibly large infrastructure, you may want to consider updating your Dropwizard config!

## Non-performance Comparison

### Safety

HikariCP claims that it is safe by default, but what does that mean?

- Connections are rolled back when returned to the pool, so any uncommited statements are not affected by the next user. It's a code smell for an application to not have auto commit or explicitly commit / rollback (eg. transaction), but HikariCP will have your back so you don't shoot yourself in the foot. Imagine an endpoint where you're executing 1000 inserts and for some reason they are not in a transaction and auto commit is disabled. Halfway through, an exception strikes and the connection is handed back to the pool. If rollback was not enabled, the next request could take that same connection, successfully insert 1000 rows, and now all of a sudden you have 1500 rows that were inserted, which would certainly be surprising! Tomcat can be configured to have this behavior by setting `rollbackOnReturn: true`
- If the connection hasn't been used in the last 500ms, HikariCP will ensure that it is valid before handing it to you. This could be helpful if the database has a max connection time, and once exceeded, the connection will be killed. Tomcat would be happy to give you this killed connection unless `checkConnectionOnBorrow: true` (for dropwizard) `testOnBorrow: true` (tomcat jdbc api). To be fair, HikariCP can hand you an invalid connection if the database failed within 500ms of the last time the connection was accessed, but in this case the database is probably down. The difference is relatively small here, if the database closed connections after 30mins Tomcat would be erroring out every 30mins, whereas HikariCP would handle the closure gracefully.
- If one was traversing a large resultset and an exception happened in the middle, HikariCP will close the statement when the connection is returned to the pool, Tomcat won't. This means that if these exceptions happen often, one could have a large number of statements in mid traversal that are hanging up the database. It is possible to register the `StatementFinalizer` jdbc interceptor on Tomcat, which will close statements on connection returns except when there is large amounts of GC pressure (due to weak references)
- One would expect that if a user modifies the auto commit, transaction level, etc of the connection that those values would be reverted once the connection is sent back to the pool. This is true for HikariCP, but not for Tomcat without registering the `ConnectionState` jdbc interceptor. Honestly, I don't know why this isn't considered a bug

HikariCP wins when it comes to safety. The fact that other pools prefer bugs by default is a bit mind boggling. 

A by product of being safe by default is that HikariCP configuration can be quite short!

```yaml
datasourceClassName: org.postgresql.ds.PGSimpleDataSource
properties:
  'databaseName': 'postgres'
user: ${user}
maxSize: ${poolSize}
password: ${password}
```

### Metrics

One of the highlights of using the Dropwizard framework is the integration of
[Dropwizard Metrics](http://metrics.dropwizard.io/), which can send metrics to
any number of places for further analysis. I know, personally, the metrics
exposed through Dropwizard Metrics has saved my skin multiple times, by either
being able to point the blame somewhere else or catch internal problem before
it becomes noticeable :smile:

Whereas all Tomcat metrics are done from the Dropwizard framework on the
outside looking in, HikariCP works with Dropwizard metrics internally and can
expose more meaningful metrics (as well as the same metrics reported with
Tomcat).

Both report:

 - Number of connections active / idle / total
 - Number of threads waiting for a connection

But only HikariCP reports:

 - Durations to create a connection
 - Durations to obtain a connection (eg. if the connection had already been created and is sitting in the pool)
 - How frequently connections are obtained
 - How long connections are used before being returned to the pool
 - When connections timeout

Which adds more insight into where applications spend their time.

Tomcat does report more metrics via JMX but these are not exposed in Dropwizard applications and aren't as useful:

 - How many total connections created / borrowed / returned / released / reconnected

When it comes to metrics, HikariCP wins hands down with the deep integration
with metric libraries that allow HikariCP to expose inner workings leading to
more transparency. As long as Tomcat remains a black box, one will have to rely
on creating their own metrics based on the API.

### Healthchecks

It's a small plus, but HikariCP is able to register healthchecks into the
Dropwizard framework. Dropwizard already registers a healthcheck that sends a
dummy query to the server, so it's a little bit redundant. Though, if
healthchecks are added in the future, Dropwizard users would get to benefit for
free.

## Benchmark Improvements

This benchmark is not perfect and can be improved in the following way.

- Database pools would prefer to have a fixed number of connections (like we have in this benchmark), but some people may want to start small and only allocate connections as needed, even at the cost of some performance.
- This benchmark only tests one endpoint that makes one small select statement. Here are some ideas for more endpoints:
  - An endpoint that makes many small selects
  - An endpoint that makes one large select
  - An endpoint that does an insert, update, delete
  - An endpoint that wraps several statements in one transaction
- These test really shouldn't ran in a virtualized environment on my dev machine
- The database and web server proably won't be deployed side by side in a production app
- Create an even lighter endpoint that performs a single request (the db pool should have more of an impact).
