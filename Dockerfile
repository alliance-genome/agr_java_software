ARG ALLIANCE_RELEASE=latest
FROM agrdocker/agr_base_linux_env:${ALLIANCE_RELEASE}

WORKDIR /workdir/agr_java_software

ADD . .

RUN mvn -T 4 -B clean package
