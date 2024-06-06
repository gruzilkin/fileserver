package com.gruzilkin.fileserver.blockstorage.data.cassandra.repository;

import com.gruzilkin.fileserver.blockstorage.data.cassandra.HashToId;
import org.springframework.data.repository.CrudRepository;

public interface HashToIdRepository extends CrudRepository<HashToId, String> {
}
