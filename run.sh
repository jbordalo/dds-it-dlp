docker network remove dds-network
docker network create -d bridge dds-network --subnet=172.26.0.0/16
# docker volume create dds-vol
gnome-terminal -- docker run -v dds-vol:/mnt/vol/ -e "PORT=8080" -e "REPLICA_ID=0" --network=dds-network -p 8080:8080 --ip=172.26.0.2 -t jc/dds-it-dlp &
gnome-terminal -- docker run -v dds-vol:/mnt/vol/ -e "PORT=8081" -e "REPLICA_ID=1" --network=dds-network -p 8081:8081 --ip=172.26.0.3 -t jc/dds-it-dlp &
gnome-terminal -- docker run -v dds-vol:/mnt/vol/ -e "PORT=8082" -e "REPLICA_ID=2" --network=dds-network -p 8082:8082 --ip=172.26.0.4 -t jc/dds-it-dlp &
gnome-terminal -- docker run -v dds-vol:/mnt/vol/ -e "PORT=8083" -e "REPLICA_ID=3" --network=dds-network -p 8083:8083 --ip=172.26.0.5 -t jc/dds-it-dlp &

