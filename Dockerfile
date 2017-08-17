FROM agrdocker/agr_api_env:develop

WORKDIR /workdir/agr_indexer

ADD . .
RUN make

CMD make run
