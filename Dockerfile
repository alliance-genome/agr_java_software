FROM agrdocker/agr_java_env:latest

WORKDIR /workdir/agr_elasticsearch_util

RUN mkdir /root/.m2
ARG PASSWORD=mypassword
ENV PASSWORD ${PASSWORD}
RUN echo "<settingsSecurity><master>`mvn --encrypt-master-password $PASSWORD`</master></settingsSecurity>" > /root/.m2/settings-security.xml
ADD settings.xml /root/.m2
ADD . .

ARG VERSION=1.0.0
ENV VERSION ${VERSION}
RUN mvn versions:set -DnewVersion=$VERSION

RUN mvn clean package

CMD mvn deploy
