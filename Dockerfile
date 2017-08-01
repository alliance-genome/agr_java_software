FROM agrdocker/agr_api_env:latest

WORKDIR /workdir/agr_api

ADD . .
RUN make

CMD make run
