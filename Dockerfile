FROM agrdocker/agr_java_env:develop

WORKDIR /workdir/agr_indexer

ADD . .
RUN make

CMD make run
