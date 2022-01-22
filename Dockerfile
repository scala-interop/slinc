FROM docker.io/library/openjdk:17-jdk-slim

RUN apt-get update && apt-get -y install curl gcc git