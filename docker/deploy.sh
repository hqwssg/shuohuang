#!/bin/sh

usage() {
	echo "Usage: sh deploy.sh [port|base|app|screen|all|stop|rm]"
	exit 1
}

port(){
	firewall-cmd --add-port=80/tcp --permanent
	firewall-cmd --add-port=3000/tcp --permanent
	firewall-cmd --add-port=3306/tcp --permanent
	service firewalld restart
}

base(){
	docker-compose up -d ruoyi-db
}

app(){
	docker-compose up -d ruoyi-web ruoyi-screen
}

screen(){
	docker-compose up -d ruoyi-screen
}

all(){
	docker-compose up -d ruoyi-db ruoyi-web ruoyi-screen
}

stop(){
	docker-compose stop
}

rm(){
	docker-compose rm
}

case "$1" in
"port")
	port
;;
"base")
	base
;;
"app")
	app
;;
"screen")
	screen
;;
"all")
	all
;;
"stop")
	stop
;;
"rm")
	rm
;;
*)
	usage
;;
esac
