FROM openjdk:17-jdk-slim

RUN apt-get update && apt-get -y install curl git
ADD https://git.io/coursier-cli-linux /usr/local/bin/cs
RUN chmod +x /usr/local/bin/cs
ADD https://github.com/com-lihaoyi/mill/releases/download/0.9.9/0.9.9 /usr/local/bin/mill
RUN chmod +x /usr/local/bin/mill
ENV COURSIER_INSTALL_DIR /usr/local/bin/
RUN cs install sbt
RUN cs install bloop
RUN sbt -Dsbt.rootdir=true version

ADD development-container/.bloop/ /root/.bloop/