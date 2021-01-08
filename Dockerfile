ARG ALLIANCE_RELEASE=latest
ARG REG=100225593120.dkr.ecr.us-east-1.amazonaws.com

FROM ${REG}/agr_base_linux_env:${ALLIANCE_RELEASE}

WORKDIR /workdir/agr_java_software

ADD . .

RUN mvn -T 4 -B clean package
