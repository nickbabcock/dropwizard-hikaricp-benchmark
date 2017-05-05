package com.example;

import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.validation.BaseValidator;
import org.junit.Test;

import java.io.IOException;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class HikariDataSourceFactoryTest {
    @Test
    public void canDeserializeCorrectly() throws IOException, ConfigurationException {
        final HikariDataSourceFactory factory = new YamlConfigurationFactory<>(HikariDataSourceFactory.class,
                BaseValidator.newValidator(), Jackson.newObjectMapper(), "dw")
                .build(new ResourceConfigurationSourceProvider(), "config.yaml");

        assertThat(factory.getUser()).isEqualTo("nick");
        assertThat(factory.getPassword()).isEqualTo("nickss");
        assertThat(factory.getDatasourceClassName()).isEqualTo("org.postgresql.ds.PGSimpleDataSource");
        assertThat(factory.getProperties()).containsExactly(entry("databaseName", "postgres"));
        assertThat(factory.getMinSize()).isEqualTo(OptionalInt.empty());
        assertThat(factory.getMaxSize()).isEqualTo(12);
        assertThat(factory.isAutoCommit()).isTrue();
    }
}