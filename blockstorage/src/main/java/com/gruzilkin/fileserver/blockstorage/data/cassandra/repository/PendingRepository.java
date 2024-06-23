package com.gruzilkin.fileserver.blockstorage.data.cassandra.repository;

import com.gruzilkin.fileserver.blockstorage.data.cassandra.Pending;
import org.springframework.data.repository.CrudRepository;

public interface PendingRepository extends CrudRepository<Pending, String> {
}
