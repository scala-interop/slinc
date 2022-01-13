FROM docker.io/library/openjdk:17-jdk-slim

RUN apt-get update && apt-get -y install curl gcc
ADD https://github.com/com-lihaoyi/mill/releases/download/0.10.0-M5/0.10.0-M5 /usr/local/bin/mill
RUN chmod +x /usr/local/bin/mill

ADD development-container/.bloop/ /root/.bloop/