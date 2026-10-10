# syntax=docker/dockerfile:1
# Self-contained runtime: avoid dependency on a separately published GHCR base package.
FROM ubuntu:24.04
ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       openjdk-21-jre-headless libreoffice-nogui fontconfig \
       fonts-noto-cjk fonts-liberation fonts-wqy-microhei fonts-wqy-zenhei \
       tzdata locales \
    && locale-gen zh_CN.UTF-8 \
    && apt-get clean && rm -rf /var/lib/apt/lists/*
COPY docker/kkfileview-base/fonts/ /usr/local/share/fonts/eap/
RUN fc-cache -f
WORKDIR /opt/eap-fileview
# Copy Maven runtime jars resolved by the release build.
COPY server/target/dependency/ ./lib/
ENV LANG=zh_CN.UTF-8 LC_ALL=zh_CN.UTF-8
ARG APP_VERSION=3.0.2
COPY server/target/eap-fileview-${APP_VERSION}.jar ./app.jar
COPY server/src/main/config/application.properties ./config/application.properties
ENV KK_CONTEXT_PATH=/fileview KK_SERVER_PORT=8012 KK_FILE_DIR=/opt/eap-fileview/file
EXPOSE 8012
ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-Dspring.config.location=/opt/eap-fileview/config/application.properties","-cp","/opt/eap-fileview/app.jar:/opt/eap-fileview/lib/*","cn.keking.ServerMain"]
