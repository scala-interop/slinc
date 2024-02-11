FROM ubuntu:jammy

RUN useradd -ms /bin/bash developer
ADD https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz cs.gz
RUN apt update && apt install git gzip git-lfs -y && rm -rf /var/lib/apt/lists/*
RUN gzip -d cs.gz 
RUN chmod +x cs
USER developer
RUN /cs --setup
RUN /cs setup --jvm temurin:1.21 --yes