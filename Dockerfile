ARG REG=100225593120.dkr.ecr.us-east-1.amazonaws.com
ARG DOCKER_IMAGE_TAG=latest

FROM ${REG}/agr_base_linux_env:${DOCKER_IMAGE_TAG}

WORKDIR /workdir/agr_java_software

ADD . .

RUN mvn -T 4 -B clean package
