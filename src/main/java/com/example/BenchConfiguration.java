package com.example;

import io.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import java.util.Optional;

public class BenchConfiguration extends Configuration {
    @Valid
    private Optional<DataSourceFactory> tomcatFactory = Optional.empty();

    @Valid
    private Optional<HikariDataSourceFactory> hikariFactory = Optional.empty();

    @JsonProperty("tomcat")
    public void setTomcatFactory(Optional<DataSourceFactory> factory) {
        this.tomcatFactory = factory;
    }

    @JsonProperty("tomcat")
    public Optional<DataSourceFactory> getTomcatFactory() {
        return tomcatFactory;
    }

    @JsonProperty("hikari")
    public Optional<HikariDataSourceFactory> getHikariFactory() {
        return hikariFactory;
    }

    @JsonProperty("hikari")
    public void setHikariFactory(Optional<HikariDataSourceFactory> hikariFactory) {
        this.hikariFactory = hikariFactory;
    }
}
