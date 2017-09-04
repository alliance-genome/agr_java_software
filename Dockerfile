FROM agrdocker/agr_java_env:latest

WORKDIR /workdir/agr_indexer

ADD . .
RUN make

CMD make run
