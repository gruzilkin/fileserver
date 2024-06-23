package com.gruzilkin.fileserver.blockstorage;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.io.IOException;

@SpringBootApplication
@EnableCaching
public class BlockstorageApplication {

	public static void main(String[] args) throws IOException, InterruptedException {
		var context = SpringApplication.run(BlockstorageApplication.class, args);
		Server server = ServerBuilder.forPort(9090)
				.addService(context.getBean(BlockStorageServiceGrpcImpl.class))
				.build();

		server.start();

		System.out.println("Block Storage gRPC service is up");

		server.awaitTermination();
	}

}
