package com.gruzilkin.fileserver.metadata;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.IOException;

@SpringBootApplication
@EnableJpaRepositories
public class MetadataApplication {

	public static void main(String[] args) throws IOException, InterruptedException {
		var context = SpringApplication.run(MetadataApplication.class, args);
		Server server = ServerBuilder.forPort(9090)
				.addService(context.getBean(MetaStorageService.class))
				.build();

		server.start();

		System.out.println("Meta Storage gRPC service is up");

		server.awaitTermination();
	}

}
