# Base image for WAAAGH!!! Cursor Cloud Agents.
#
# JDK 11 builds the IntelliJ plugin (Gradle 7.4.2 does not support JDK 21).
# JDK 17 is the ideal Project SDK for the Maven sample fixture
# (waaagh/sample/debug_openfeign, which targets Java 17).
FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        openjdk-11-jdk \
        openjdk-17-jdk \
        git \
        curl \
        unzip \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Default JAVA_HOME = JDK 11 for the Gradle plugin build.
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"
