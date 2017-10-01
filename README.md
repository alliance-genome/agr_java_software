# AGR API Documentation

This repo is used to provide a public interface. The main things that it provides is access to our internal resources. Right now the main resources are our search index and our graph database.

## [GraphQL](http://graphql.org/learn/)

In order to provide the user with direct access to query our main database the API provides a GraphQL interface. This is a popular interface for providing an API to datbases for Websites. This interface can also be used for other purposes as well.

## [GraphiQL](https://github.com/graphql/graphiql)

It also provides a web interface for performing GraphQL queries. The interface itself provides autocompletion and is schema aware. 

## Search endpoints

This interface provides a means of communicating with our [ElasticSearch](https://info.elastic.co) search index. This endpoint is intended to be mainly used by the AGR search interface.

## Future

As we add more datasources and use the ElasticSearch indexing for storing / caching table information, the API will provide endpoints to access the data in these tables. Some of the features that it will provide is pagination, sorting, facets, and filtering results. 

# Usage

## Compliling JAR Without Docker

```bash
make
```

## Running API Without Docker

```bash
make run
```

## Compliling JAR and building the Docker Image

```bash
make docker-build
```

## Running API Inside of Docker

```bash
make docker-run
```

# Configuration

In order to configure the running of the app modify the values in the app.properties file. 

# Endpoints
| URL | description |
| --- | ----------- |
| /api/disease/\<disease term ID\> | retrieve disease info for a given term ID (JSON) |
| /api/gene/\<gene ID\> | retrieve gene info for a given gene ID (JSON) |
| /api/disease/\<disease term ID\>/associations | get all disease annotations for a given disease term ID (JSON) |
| /api/disease/\<disease term ID\>/associations/download | retrieve all disease annotations for a given disease term ID in tab delimited format |
