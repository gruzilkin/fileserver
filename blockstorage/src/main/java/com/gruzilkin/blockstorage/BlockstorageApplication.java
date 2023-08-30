package com.gruzilkin.blockstorage;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class BlockstorageApplication {

	public static void main(String[] args) throws IOException, InterruptedException {
		var context = SpringApplication.run(BlockstorageApplication.class, args);
		Server server = ServerBuilder.forPort(9090)
				.addService(context.getBean(BlockStorageService.class))
				.build();

		server.start();

		System.out.println("gRPC boot");

		server.awaitTermination();
	}

}
