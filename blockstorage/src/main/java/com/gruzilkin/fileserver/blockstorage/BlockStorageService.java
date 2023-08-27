package com.gruzilkin.fileserver.blockstorage;

import com.gruzilkin.common.HelloRequest;
import com.gruzilkin.common.HelloResponse;
import com.gruzilkin.common.HelloServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

@Component
public class BlockStorageService extends HelloServiceGrpc.HelloServiceImplBase {

    @Override
    public void hello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        String message = "Hello " + request.getFirstName() + " " + request.getLastName();
        HelloResponse rs = HelloResponse.newBuilder().setGreeting(message).build();
        responseObserver.onNext(rs);
        responseObserver.onCompleted();
    }
}
