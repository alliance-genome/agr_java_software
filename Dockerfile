FROM agrdocker/agr_java_env:develop

WORKDIR /workdir/agr_api

ADD . .
RUN make

CMD make run
