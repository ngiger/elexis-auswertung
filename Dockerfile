FROM openjdk:8-jre
MAINTAINER Niklaus Giger "elexis.giger@member.fsf.org"
RUN apt update
RUN apt install -y xvfb
# Next line are some nice utilities for debugging
RUN apt install -y mysql-client procps
