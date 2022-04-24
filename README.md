# Intrusion-Tolerant Decentralized Ledger Platform (IT-DLP)


## Getting Started


`git clone https://github.com/jbordalo/dds-it-dlp.git`

`cd dds-it-dlp`

<br>

## Building

`docker build -t jc/dds-it-dlp .`

or

`./build.sh`

<br>

## Running the servers

```
docker network remove dds-network
docker network create -d bridge dds-network --subnet=172.26.0.0/16
docker run -e "PORT=<PORT>" -e "REPLICA_ID=<REPLICA_ID>" --network=dds-network -p 8080:<PORT> --ip=<REPLICA_IP> -t jc/dds-it-dlp
```

or

`./run.sh`


### Example run


Building with provided script `build.sh`.

`$ ./build.sh`

Running with provided script `run.sh`.

Launch 4 replicas.

`$ ./run.sh`
```
docker network remove dds-network
docker network create -d bridge dds-network --subnet=172.26.0.0/16
gnome-terminal -- docker run -e "PORT=8080" -e "REPLICA_ID=0" --network=dds-network -p 8080:8080 --ip=172.26.0.2 -t jc/dds-it-dlp &
gnome-terminal -- docker run -e "PORT=8081" -e "REPLICA_ID=1" --network=dds-network -p 8081:8081 --ip=172.26.0.3 -t jc/dds-it-dlp &
gnome-terminal -- docker run -e "PORT=8082" -e "REPLICA_ID=2" --network=dds-network -p 8082:8082 --ip=172.26.0.4 -t jc/dds-it-dlp &
gnome-terminal -- docker run -e "PORT=8083" -e "REPLICA_ID=3" --network=dds-network -p 8083:8083 --ip=172.26.0.5 -t jc/dds-it-dlp &
```

## Configuration
The replica ip addresses and ids must match the file `config/hosts-docker.config`.

## Running the client

`./gradlew :client`

## Authors

-   **João Bordalo** - _Initial work_ - [jbordalo](https://github.com/jbordalo)
-   **Jacinta Sousa** - _Initial work_ - [jacintinha](https://github.com/jacintinha)