FROM agrdocker/agr_api_env:develop

WORKDIR /workdir/agr_api

ADD . .
RUN make

CMD make run
