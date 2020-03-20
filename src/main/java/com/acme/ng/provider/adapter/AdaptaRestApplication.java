package com.acme.ng.provider.adapter;

import com.acme.coreops.so.nextgen.afd.config.AfdClientConfig;
import com.acme.ng.provider.adapter.common.repository.destination.rdbms.RelationalDatabaseRepositoryImpl;
import com.acme.ng.provider.adapter.config.ApplicationConfig;
//import com.acme.ng.provider.adapter.context.AdapterContext;
//import com.acme.ng.provider.adapter.context.ptdm.PtdmAdapterContext;
import com.acme.ng.provider.adapter.config.TableConfig;
import com.acme.ng.provider.adapter.rest.extension.service.service.NestedTableWriter;
import com.acme.ng.provider.adapter.rest.extension.service.service.test.NeoUpload;
import com.acme.ng.provider.common.repository.graph.manager.CgnTransactionManager;
//import com.acme.ng.provider.config.ObjectMapperConfig;
import com.acme.ng.provider.dispatcher.client.config.DispatcherClientConfig;
import com.acme.ng.provider.adapter.rest.extension.service.service.TableWriterOld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.StreamSupport;

@SpringBootApplication
@ComponentScan(value = {
        "com.acme.ng.provider.adapter.scheduler," +
        "com.acme.ng.provider.adapter.batch," +
        "com.acme.ng.provider.adapter.service.impl," +
        "com.acme.ng.provider.adapter.batch.config," +
        "com.acme.ng.provider.adapter.batch.core," +
        "com.acme.ng.provider.adapter.common.service," +
        "com.acme.ng.provider.common.adapter," +
        "com.acme.ng.provider.adapter.controller," +
        "com.acme.ng.provider.common.service," +
        "com.acme.ng.provider.context," +
        "com.acme.ng.provider.utils," +
        "com.acme.ng.provider.common.repository," +
        "com.acme.ng.provider.adapter.poller," +
        "com.acme.ng.provider.adapter.strategy," +
        "com.acme.ng.provider.aspects," +
        "com.acme.ng.provider.adapter.rest.extension.service," +
        "com.acme.ng.provider.adapter.rest.extension.model," +
        "com.acme.ng.provider.adapter.rest.extension.controller," +
        "com.acme.ng.provider.adapter.common.service.columns.impl"
        }, excludeFilters = @Filter(type = FilterType.REGEX,
        pattern = {
                "com.acme.ng.provider.adapter.context.*",
                "com.acme.ng.provider.adapter.common.repository.destination.*"
        }))
@Import(value = {
        TableConfig.class,
        AfdClientConfig.class,
        DispatcherClientConfig.class,
        //ObjectMapperConfig.class,
        MessagesConfig.class
})
public class AdaptaRestApplication implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(AdaptaRestApplication.class);

    @Value("${events.pool-size}")
    private int poolSize;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Autowired
    private Environment environment;

    @Autowired
    private TableWriterOld tblWriter;

    @Autowired
    private NestedTableWriter nstdTblWriter;

    @Autowired
    private NeoUpload neoUploader;

    public static void main(String[] args) {
        SpringApplication.run(AdaptaRestApplication.class, args);
    }

    @Override
    public void run(String... args) {
        //tblWriter.run();
        neoUploader.run();
    }

    @Configuration
    @EnableTransactionManagement
    @ConditionalOnProperty(name = "destination.repository.type", havingValue = "rdbms")
    @ComponentScan(basePackageClasses = RelationalDatabaseRepositoryImpl.class)
    @Import(ApplicationConfig.class)
    public class RelationalDestinationDatabaseConfiguration {

        @Bean(destroyMethod = "shutdown")
        public ExecutorService executorService() {
            return Executors.newFixedThreadPool(poolSize);
        }

        @Bean
        public CgnTransactionManager cgnTransactionManager() {
            return new CgnTransactionManager();
        }


    }

    @Configuration
    public class NextgenDatabaseConfiguration {
        // TODO datasource for nextgen MongoDB
    }

    @PostConstruct
    public void printProfile() {
        LOG.debug("Active profile:" + activeProfile);
        if (activeProfile != null && (activeProfile.contains("local") || activeProfile.contains("ode"))) {
            final MutablePropertySources propertySources = ((AbstractEnvironment) environment).getPropertySources();
            StreamSupport.stream(propertySources.spliterator(), false)
                    .filter(ps -> ps instanceof EnumerablePropertySource)
                    .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
                    .flatMap(Arrays::<String>stream)
                    .forEach(name -> LOG.debug(name + " : " + environment.getProperty(name)));
        }
    }
}
