package com.gruzilkin.fileserver.metadata.data.repository;

import com.gruzilkin.fileserver.metadata.data.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
}
