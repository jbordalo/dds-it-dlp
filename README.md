# Intrusion-Tolerant Decentralized Ledger Platform (IT-DLP)


## Getting Started


`git clone https://github.com/jbordalo/dds-it-dlp.git`

`cd dds-it-dlp`


## Building

`docker build -t jc/dds-it-dlp .`

`docker build -f DockerfileEndorser -t jc/dds-it-dlp-endorser`

or

`./build.sh`

## Running the servers

### BFTSMaRt
`docker-compose up`

### Blockmess
`docker-compose -f docker-compose-blockmess.yml up`

## Configuration
The replica ip addresses and ids in `docker-compose.yml` must match the file `config/hosts-docker.config`.

The `contact` property in file `config/config-docker.properties` must match the ip address and port of the first replica. 

## Running the test client

`./gradlew :testClient`

## Running an interactive client

`./gradlew :interactiveClient`

## Running YCSB

`./gradlew :benchmark --args="-P config/workloads/workloada"`

## Authors

-   **João Bordalo** - _Initial work_ - [jbordalo](https://github.com/jbordalo)
-   **Jacinta Sousa** - _Initial work_ - [jacintinha](https://github.com/jacintinha)