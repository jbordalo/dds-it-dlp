docker network create -d bridge ournet --subnet=172.26.0.0/16
sudo docker run -e "PORT=8080" -e "REPLICA_ID=0" --network=ournet -p 8080:8080 -p 11000:11000 --ip=172.26.0.2 -t jc/dds-it-dlp &
sudo docker run -e "PORT=8081" -e "REPLICA_ID=1" --network=ournet -p 8081:8081 -p 11010:11010 --ip=172.26.0.3 -t jc/dds-it-dlp &
sudo docker run -e "PORT=8082" -e "REPLICA_ID=2" --network=ournet -p 8082:8082 -p 11020:11020 --ip=172.26.0.4 -t jc/dds-it-dlp &
sudo docker run -e "PORT=8083" -e "REPLICA_ID=3" --network=ournet -p 8083:8083 -p 11030:11030 --ip=172.26.0.5 -t jc/dds-it-dlp &

