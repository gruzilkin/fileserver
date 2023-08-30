package com.gruzilkin.blockstorage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.util.List;

@Configuration
@EnableCassandraRepositories(basePackages = "com.gruzilkin.blockstorage.data.cassandra.repository")
public class CassandraConfiguration extends AbstractCassandraConfiguration {

    @Value("${cassandra.contact.points}")
    String contactPoints;
    @Value("${cassandra.keyspace.name}")
    String keyspaceName;

    /*
     * Provide a contact point to the configuration.
     */
    @Override
    public String getContactPoints() {
        return contactPoints;
    }

    /*
     * Provide a keyspace name to the configuration.
     */
    @Override
    public String getKeyspaceName() {
        return keyspaceName;
    }

    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        CreateKeyspaceSpecification specification = CreateKeyspaceSpecification.createKeyspace(keyspaceName)
                .with(KeyspaceOption.DURABLE_WRITES, true)
                .ifNotExists();

        return List.of(specification);
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }
}
