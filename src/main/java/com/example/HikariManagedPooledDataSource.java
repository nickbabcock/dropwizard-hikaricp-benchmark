package com.example;

import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.db.ManagedDataSource;

public class HikariManagedPooledDataSource extends HikariDataSource implements ManagedDataSource {
    public HikariManagedPooledDataSource(final HikariConfig config) {
        super(config);
    }

    // JDK6 has JDBC 4.0 which doesn't have this -- don't add @Override
    @SuppressWarnings("override")
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Doesn't use java.util.logging");
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
        this.close();
    }
}
