FROM docker.io/library/openjdk:17-jdk-slim

ADD https://github.com/com-lihaoyi/mill/releases/download/0.10.0-M5/0.10.0-M5 /usr/local/bin/mill
RUN chmod +x /usr/local/bin/mill
