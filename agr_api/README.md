# AGR API Documentation

This repo is used to provide a public interface. The main thing that it provides is access to internal resources. The main resources are the search index and graph database.


## Getting Started

This repo will get a running API the project, using JBoss Swarm. 


## Contents

- [Configuring](#configuring)
  * [Parameters](#parameters)
- [Running API](#running-api)
- [Docker](#docker)
- [Endpoints](#endpoints)
- [Maintainers](#maintainers)

## Configuring

### Parameters
#### API\_ACCESS_TOKEN
This is a token that all API requests must use as a auth header, if set then its required otherwise the API will not require the token.
#### DEBUG
Send extra debugging information into the logs.
#### ES_INDEX
This is the name of the index used by the API to get all its information.
#### ES_HOST
This is the host that the API will attempt to connect to, for getting its data.
#### ES_PORT
This is the port number on ES_HOST that will be used for the Elastic Search connection
#### swarm.logging
This is the default log level for the output of log4j in the log files

An example of setting these params:

	#> export ES_HOST=http://www.example.com
	#> export ES_PORT=9300
	#> export DEUBG=true
	
Also these paramaters can be put in a file called app.properties

## Running API

The following commands should build and run the API:

	#> mvn clean package
	#> java -jar target/agr_api-swarm.jar -Papp.properties
	
### API Documentation

After the API is up and running, API documentation can be found through swagger via the following url assuming the API is running on localhost. [http://localhost:8080/api](http://localhost:8080/api)

## Docker

### Creating a docker container

	#> docker build -t agr_api_server .
	
### Runing the docker container

	#> docker run -p 8080:8080 -t -i agr_api_server

## Endpoints
| URL | description |
| --- | ----------- |
| /api/disease/\<disease term ID\> | retrieve disease info for a given term ID (JSON) |
| /api/gene/\<gene ID\> | retrieve gene info for a given gene ID (JSON) |
| /api/disease/\<disease term ID\>/associations | get all disease annotations for a given disease term ID (JSON) |
| /api/disease/\<disease term ID\>/associations/download | retrieve all disease annotations for a given disease term ID in tab delimited format |

## Maintainers

Current maintainers:

 * [AGR Software Team](https://github.com/orgs/alliance-genome/teams/software)
