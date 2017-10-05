# niord-gh-proxy

*niord-gh-proxy* is a simplified version of the [niord-proxy](https://github.com/NiordOrg/niord-proxy)
web-application, used for showing active NW + NM messages to end users.

It will fetch the active NW + NM messages from a running instance of the *niord-gh-web* web
application.

## Prerequisites

* Java 8
* Maven 3

## Development Set-Up

It is very easy to build and run *niord-gh-proxy* as it is packaged as a stand-alone executable
jar file.

Build and run using:

    mvn clean package
    java -Dswarm.http.port=9000 \
         -Dniord-proxy.server=http://localhost:8080 \
         -jar target/niord-gh-proxy-swarm.jar

For a full range of settings, see the *Settings* class.

Alternatively, you can run the *NiordProxyMain* class from withing e.g. IntelliJ with the
following configuration:
* Use "-Dswarm.http.port=9000 -Dniord-proxy.server=http://localhost:8080" as VM Options.
* Ensure that *niord-gh-proxy* is used as the working directory.

## Creating a Docker Release

First, build the *niord-gh-proxy* war:

    mvn clean install
    
Next, build the docker image:

    cd ../docker/
    ./build-niord-gh-proxy.sh build ../target/niord-gh-proxy-swarm.jar
     
    # If successful, and if you have previously used "docker login", push to dockerhub:
    ./build-niord-gh-proxy.sh push
