
cd vào bên trong folder docker-compose

docker-compose -f common.yml -f kafka_cluster.yml up -d

  # Tìm network của Kafka containers
  docker inspect docker-compose-kafka-broker-1-1 | findstr NetworkMode
  # hoặc
  docker network ls
  docker inspect <network-name>

# Chạy Kafka UI trong cùng network
docker run -p 7070:8080 -d --network <kafka-network-name> -e KAFKA_CLUSTERS_0_NAME=local -e KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=docker-compose-kafka-broker-1-1:9092,docker-compose-kafka-broker-2-1:9092,docker-compose-kafka-broker-3-1:9092 provectuslabs/kafka-ui:latest
docker run -p 7070:8080 -d --network docker-compose_application -e KAFKA_CLUSTERS_0_NAME=local -e KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=docker-compose-kafka-broker-1-1:9092,docker-compose-kafka-broker-2-1:9092,docker-compose-kafka-broker-3-1:9092 provectuslabs/kafka-ui:latest


# config trong bootstrap.yml của config server
uri: file:///D:/Phu/PhuObs/udemy/Event-Driven-Microservices-Spring-Boot,-Kafka-and-Elastic/microservices-demo/config-server-repository

# tắt port 8888 = powershell
Get-NetTCPConnection -LocalPort 8888 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
