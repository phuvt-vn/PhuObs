//nginx with a path html 
docker run --name nginx --hostname ng1 -p 80:80 -v /Users/HusseinNasser/javascript/javascript_playground/nginx-udemy-container/html:/usr/share/nginx/html -d nginx

//inspect ccontainer 
docker inspect nginx

//vanilla nginx 
docker run --name nginx --hostname ng1 -p 80:80 -d nginx

//create  nodeapp 
docker build . -t nodeapp
docker run --hostname nodeapp1 --name nodeapp1 -d nodeapp
docker run --hostname nodeapp2 --name nodeapp2 -d nodeapp
docker run --hostname nodeapp3 --name nodeapp3 -d nodeapp


docker run --hostname ng1 --name nginx -p 80:8080 -v /Users/HusseinNasser/javascript/javascript_playground/nginx-udemy-container/nginx.conf:/etc/nginx/nginx.conf -d nginx

//create a docker network
docker network create backendnet
docker network connect backendnet nodeapp1
docker network connect backendnet nodeapp2
docker network connect backendnet nodeapp3
docker network connect backendnet nginx





//spin up another NGINX instance

docker run --hostname ng2 --name nginx -p 81:8080 -v /Users/HusseinNasser/javascript/javascript_playground/nginx-udemy-container/nginx.conf:/etc/nginx/nginx.conf -d ng2

docker network connect backendnet ng2

//use ip tables to load balance connections to port 80/81 from 82 this is all outside docker, ip address is 192.168.254.10 

sudo iptables --table nat --append PREROUTING --destination 192.168.254.10 --protocol tcp --dport 82 --match statistic --mode nth --every 3 --packet 0 --jump DNAT --to-destination 192.168.254.10:80

sudo iptables --table nat --append PREROUTING --destination 192.168.254.10 --protocol tcp --dport 82 --match statistic --mode nth --every 2 --packet 0 --jump DNAT --to-destination 192.168.254.10:81



//SNAT
sudo iptables --table nat --append POSTROUTING --destination 192.168.254.10 --protocol tcp --dport 81 --jump SNAT --to-source 192.168.254.10

sudo iptables --table nat --append POSTROUTING --destination 192.168.254.10 --protocol tcp --dport 82 --jump SNAT --to-source 192.168.254.10









//DEMO JAVA
docker build . -t javaapp
docker run --hostname javaapp1 --name javaapp1 -d javaapp
docker run --hostname javaapp2 --name javaapp2 -d javaapp
docker run --hostname javaapp3 --name javaapp3 -d javaapp


docker run --hostname ng1 --name nginx -p 80:8080 -v D:\Udemy\Introduction-to-NGINX\nginx-udemy-container\nginx.conf:/etc/nginx/nginx.conf -d nginx

//create a docker network
docker network create backendnet
docker network connect backendnet javaapp1
docker network connect backendnet javaapp2
docker network connect backendnet javaapp3
docker network connect backendnet nginx
docker start nginx
docker logs nginx

// new nginx2 connect to exist network
docker run --hostname ng2 --name ng2 -p 81:8080 -v D:\Udemy\Introduction-to-NGINX\nginx-udemy-container\nginx.conf:/etc/nginx/nginx.conf -d nginx

docker network connect backendnet ng2
docker stop ng2
docker start ng2
docker logs ng2