package com.acme.ng.provider.adapter.config;

import com.acme.ng.provider.adapter.common.model.SQLDialect;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.neo4j.driver.v1.Config.build;

/**
 * Created by Laura 
 *
 * @version 1.0
 * @date November 19, 2018.
 */
@Configuration
public class ApplicationConfig {

  @Bean
  public SQLDialect getSQLDialect(@Value("${destination.repository.rdbms.dialect}") String dialect) {
    return SQLDialect.valueOf(dialect.toUpperCase());
  }

  @Bean
  @Primary
  @ConfigurationProperties("destination.repository.rdbms.configuration")
  public DataSource rdbmsDataSource() {

      DataSourceProperties d = rdbmsDataSourceProperties();
    return d.initializeDataSourceBuilder().build();

  }


  @Bean
  @Primary
  @ConfigurationProperties("destination.repository.rdbms")
  public DataSourceProperties rdbmsDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(name = "ptdm_sim")
  @Primary
  public JdbcTemplate getPTDMJdbcTemplate(@Qualifier("rdbmsDataSource") final DataSource dataSource) throws SQLException {
    return new JdbcTemplate(rdbmsDataSource());

  }

}
