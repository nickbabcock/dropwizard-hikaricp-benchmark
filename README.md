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

![](https://github.com/nickbabcock/dropwizard-hikaricp-benchmark/raw/master/img/top-response-latencies.png)

![](https://github.com/nickbabcock/dropwizard-hikaricp-benchmark/raw/master/img/top-response-throughput.png)

