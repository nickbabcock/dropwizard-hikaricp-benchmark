package com.example;

import com.codahale.metrics.annotation.Timed;
import com.example.api.Question;
import com.example.db.QuestionMapper;
import com.example.db.QuestionQuery;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.skife.jdbi.v2.DBI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;

public class BenchApplication extends Application<BenchConfiguration> {

    public static void main(final String[] args) throws Exception {
        new BenchApplication().run(args);
    }

    @Override
    public String getName() {
        return "dropwizard-hikari-benchmark";
    }

    @Override
    public void initialize(final Bootstrap<BenchConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(true)
                )
        );
    }

    @Override
    public void run(final BenchConfiguration config,
                    final Environment environment) {
        final DBIFactory factory = new DBIFactory();
        final Optional<PooledDataSourceFactory> tomcatFactory = config.getTomcatFactory().map(x -> x);
        final Optional<PooledDataSourceFactory> hikariFactory = config.getHikariFactory().map(x -> x);
        final PooledDataSourceFactory datasource = tomcatFactory.orElse(hikariFactory.orElse(null));
        final DBI jdbi = factory.build(environment, datasource, "postgresql");
        jdbi.registerMapper(new QuestionMapper());
        final QuestionQuery dao = jdbi.onDemand(QuestionQuery.class);
        environment.jersey().register(new QuestionResource(dao));
    }

    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public class QuestionResource {
        private final QuestionQuery dao;
        public QuestionResource(QuestionQuery dao) {
            this.dao = dao;
        }

        @GET
        @Timed
        public List<Question> findQuestionsFromUser(@QueryParam("user") int userid) {
            return dao.findQuestionsFromUser(userid);
        }
    }
}
