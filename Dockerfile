ARG REG=100225593120.dkr.ecr.us-east-1.amazonaws.com
ARG DOCKER_PULL_TAG=stage

FROM ${REG}/agr_base_linux_env:${DOCKER_PULL_TAG}

WORKDIR /workdir/agr_java_software

COPY /workdir/agr_java_software/agr_api/src/main/resources/application.properties.defaults /workdir/agr_java_software/agr_api/src/main/resources/application.properties

ADD . .

RUN mvn -ntp -T 4 -B clean package
