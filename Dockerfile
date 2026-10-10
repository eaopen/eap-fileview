# syntax=docker/dockerfile:1
# The final image only carries a regular Maven JAR. Runtime libraries live in
# the versioned eap-fileview-base intermediate image and are reused downstream.
ARG BASE_IMAGE=ghcr.io/eaopen/eap-fileview-base:java21-lo-r1
FROM ${BASE_IMAGE}
ARG APP_VERSION=2.9.2
WORKDIR /opt/eap-fileview
COPY server/target/eap-fileview-${APP_VERSION}.jar ./app.jar
COPY server/src/main/config/application.properties ./config/application.properties
ENV KK_CONTEXT_PATH=/fileview KK_SERVER_PORT=8012 KK_FILE_DIR=/opt/eap-fileview/file
EXPOSE 8012
ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-Dspring.config.location=/opt/eap-fileview/config/application.properties","-cp","/opt/eap-fileview/app.jar:/opt/eap-fileview/lib/*","cn.keking.ServerMain"]
