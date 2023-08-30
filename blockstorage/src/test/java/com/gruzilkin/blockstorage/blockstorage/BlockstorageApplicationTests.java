package com.gruzilkin.blockstorage.blockstorage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnableAutoConfiguration(exclude = CassandraAutoConfiguration.class)
class BlockstorageApplicationTests {

	@Test
	void contextLoads() {
	}

}
