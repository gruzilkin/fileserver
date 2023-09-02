# cache as most as possible in this multistage dockerfile.
FROM maven:3.9-eclipse-temurin-20 as deps

WORKDIR /opt/app
COPY common/pom.xml common/pom.xml
COPY blockstorage/pom.xml blockstorage/pom.xml
COPY metadata/pom.xml metadata/pom.xml
COPY web/pom.xml web/pom.xml

# you get the idea:
# COPY moduleN/pom.xml moduleN/pom.xml

COPY pom.xml .
RUN mvn -B -e -C org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline

# if you have modules that depends each other, you may use -DexcludeArtifactIds as follows
# RUN mvn -B -e -C org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline -DexcludeArtifactIds=module1

# Copy the dependencies from the DEPS stage with the advantage
# of using docker layer caches. If something goes wrong from this
# line on, all dependencies from DEPS were already downloaded and
# stored in docker's layers.
FROM maven:3.9-eclipse-temurin-20 as builder
WORKDIR /opt/app
COPY --from=deps /root/.m2 /root/.m2
COPY --from=deps /opt/app/ /opt/app
COPY common/src /opt/app/common/src
COPY blockstorage/src /opt/app/blockstorage/src

# use -o (--offline) if you didn't need to exclude artifacts.
# if you have excluded artifacts, then remove -o flag
RUN mvn -B -e clean install -DskipTests=true

# At this point, BUILDER stage should have your .jar or whatever in some path
FROM amazoncorretto:17
WORKDIR /opt/app
COPY --from=builder /opt/app/blockstorage/target/blockstorage-1.0.0-SNAPSHOT.jar .
COPY opentelemetry-javaagent.jar .
EXPOSE 9090
CMD [ "java", "-javaagent:/opt/app/opentelemetry-javaagent.jar", "-jar", "/opt/app/blockstorage-1.0.0-SNAPSHOT.jar" ]