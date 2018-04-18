package com.example;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.zaxxer.hikari.HikariConfig;

import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.util.Duration;

public class HikariDataSourceFactory implements PooledDataSourceFactory {
    private String datasourceClassName = null;

    private String driverClass = null;

    private String url = null;

    private String user = null;

    private String password = null;

    private boolean autoCommit = true;

    @NotNull
    private Map<String, String> properties = Maps.newLinkedHashMap();

    @Min(1)
    @JsonProperty
    private OptionalInt minSize = OptionalInt.empty();

    @Min(1)
    @JsonProperty
    private int maxSize = 16;

    @NotNull
    private String validationQuery = "/* Health Check */ SELECT 1";

    private boolean autoCommentsEnabled = true;

    private HealthCheckRegistry healthCheckRegistry;

    @JsonProperty
    @Override
    public boolean isAutoCommentsEnabled() {
        return this.autoCommentsEnabled;
    }

    @JsonProperty
    public void setAutoCommentsEnabled(final boolean autoCommentsEnabled) {
        this.autoCommentsEnabled = autoCommentsEnabled;
    }

    @JsonProperty
    @Override
    public String getDriverClass() {
        return this.driverClass;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @JsonProperty
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty
    public void setDatasourceClassName(final String datasourceClassName) {
        this.datasourceClassName = datasourceClassName;
    }

    @JsonProperty
    public String getUser() {
        return this.user;
    }

    @JsonProperty
    public void setUser(final String user) {
        this.user = user;
    }

    @JsonProperty
    public String getPassword() {
        return this.password;
    }

    @JsonProperty
    public void setPassword(final String password) {
        this.password = password;
    }

    @JsonProperty
    @Override
    public Map<String, String> getProperties() {
        return this.properties;
    }

    @JsonProperty
    public void setProperties(final Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    @JsonProperty
    public String getValidationQuery() {
        return this.validationQuery;
    }

    @Override
    @Deprecated
    @JsonIgnore
    public String getHealthCheckValidationQuery() {
        return this.getValidationQuery();
    }

    @Override
    @Deprecated
    @JsonIgnore
    public Optional<Duration> getHealthCheckValidationTimeout() {
        return this.getValidationQueryTimeout();
    }

    @JsonProperty
    public boolean isAutoCommit() {
        return autoCommit;
    }

    @JsonProperty
    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    @JsonProperty
    public OptionalInt getMinSize() {
        return minSize;
    }

    @JsonProperty
    public String getDatasourceClassName() {
        return datasourceClassName;
    }

    @JsonProperty
    public void setMinSize(OptionalInt minSize) {
        this.minSize = minSize;
    }

    @JsonProperty
    public int getMaxSize() {
        return maxSize;
    }

    @JsonProperty
    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public void asSingleConnectionPool() {
        this.minSize = OptionalInt.empty();
        this.maxSize = 1;
    }

    public HealthCheckRegistry getHealthCheckRegistry() {
        return healthCheckRegistry;
    }

    public void setHealthCheckRegistry(HealthCheckRegistry healthCheckRegistry) {
        this.healthCheckRegistry = healthCheckRegistry;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    @Override
    public ManagedDataSource build(final MetricRegistry metricRegistry, final String name) {
        final Properties properties = new Properties();
        for (final Map.Entry<String, String> property : this.properties.entrySet()) {
            properties.setProperty(property.getKey(), property.getValue());
        }

        final HikariConfig config = new HikariConfig();
        config.setMetricRegistry(metricRegistry);
        if (healthCheckRegistry != null) {
            config.setHealthCheckRegistry(healthCheckRegistry);
        }

        config.setAutoCommit(autoCommit);
        config.setDataSourceProperties(properties);
        if (datasourceClassName != null) {
            config.setDataSourceClassName(datasourceClassName);
        } else {
            config.setDriverClassName(driverClass);
        }

        if (url != null) {
            config.setJdbcUrl(url);
        }

        config.setMaximumPoolSize(maxSize);
        minSize.ifPresent(config::setMinimumIdle);
        config.setPoolName(name);
        config.setUsername(user);
        config.setPassword(user != null && password == null ? "" : password);
        return new HikariManagedPooledDataSource(config);
    }

    @Override
    @JsonProperty
    public java.util.Optional<Duration> getValidationQueryTimeout() {
        return Optional.of(Duration.minutes(1));
    }
}