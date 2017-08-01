FROM agrdocker/agr_api_env:latest

WORKDIR /workdir

RUN git clone https://github.com/alliance-genome/agr_api.git

WORKDIR /workdir/agr_api

RUN make

CMD make run
