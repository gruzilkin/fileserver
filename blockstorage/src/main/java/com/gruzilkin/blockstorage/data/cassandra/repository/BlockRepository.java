package com.gruzilkin.blockstorage.data.cassandra.repository;

import com.gruzilkin.blockstorage.data.cassandra.Block;
import org.springframework.data.repository.CrudRepository;

public interface BlockRepository extends CrudRepository<Block, String> {
}
