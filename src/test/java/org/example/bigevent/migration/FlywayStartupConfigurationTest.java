package org.example.bigevent.migration;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Verifies Boot starts its migration initializer, independently of manual Flyway calls. */
class FlywayStartupConfigurationTest {
    @Test void initializerCallsConfiguredMigrationStrategyAtStartup() {
        DataSource datasource = mock(DataSource.class);
        FlywayMigrationStrategy strategy = mock(FlywayMigrationStrategy.class);
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
                .withBean(DataSource.class, () -> datasource)
                .withBean(FlywayMigrationStrategy.class, () -> strategy)
                .withPropertyValues("spring.flyway.enabled=true", "spring.flyway.locations=classpath:db/migration")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(Flyway.class).hasBean("flywayInitializer");
                    verify(strategy).migrate(context.getBean(Flyway.class));
                    // This is an auto-configuration test, deliberately not a database test.
                    verifyNoInteractions(datasource);
                });
    }
    @Test void explicitDisableDoesNotRunMigrations() {
        FlywayMigrationStrategy strategy = mock(FlywayMigrationStrategy.class);
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .withBean(FlywayMigrationStrategy.class, () -> strategy)
                .withPropertyValues("spring.flyway.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(Flyway.class);
                    verifyNoInteractions(strategy);
                });
    }
}
