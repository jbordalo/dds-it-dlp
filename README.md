# Intrusion-Tolerant Decentralized Ledger Platform (IT-DLP)


## Getting Started


`git clone https://github.com/jbordalo/dds-it-dlp.git`

`cd dds-it-dlp`


## Building

`docker build -t jc/dds-it-dlp .`

or

`./build.sh`

## Running the servers

`docker-compose up`

## Configuration
The replica ip addresses and ids in `docker-compose.yml` must match the file `config/hosts-docker.config`.

## Running the client

`./gradlew :client`

## Authors

-   **João Bordalo** - _Initial work_ - [jbordalo](https://github.com/jbordalo)
-   **Jacinta Sousa** - _Initial work_ - [jacintinha](https://github.com/jacintinha)