package com.gruzilkin.fileserver.blockstorage.data.cassandra.repository;

import com.gruzilkin.fileserver.blockstorage.data.cassandra.ColdBlock;
import org.springframework.data.repository.CrudRepository;

public interface ColdBlockRepository extends CrudRepository<ColdBlock, String> {
}
