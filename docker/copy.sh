#!/bin/sh

usage() {
	echo "Usage: sh copy.sh"
	exit 1
}

echo "begin copy sql"
mkdir -p ./mysql/db
cp ../sql/carbon_emissions.sql ./mysql/db
cp ../sql/logging_migration.sql ./mysql/db/logging_migration.sql
cp ../schema_1.sql ./mysql/db/schema_1.sql

echo "begin copy ruoyi-admin"
mkdir -p ./web-main/jar ./web-main/html/dist ./web-main/html/carbon-model ./web-main/python
cp ../ruoyi-admin/target/ruoyi-admin.jar ./web-main/jar/ruoyi-admin.jar
cp -r ../ruoyi-ui/dist/. ./web-main/html/dist/
cp "../tpfhs-9/backend/target/carbon-emission-model-1.0.0.jar" ./web-main/jar/carbon-emission-model.jar
cp -r "../tpfhs-9/frontend/dist/." ./web-main/html/carbon-model/
cp "../tpfhs-9/backend/steam_calculator.py" ./web-main/python/steam_calculator.py

echo "begin copy goview"
mkdir -p ./web-screen/jar ./web-screen/html/dist ./web-screen/sqllite ./web-screen/upload
cp ../goview/go-view-serve-master/target/goview_admin-0.0.1-SNAPSHOT.war ./web-screen/jar/goview.war
cp -r ../goview/go-view-master-fetch/dist/. ./web-screen/html/dist/
cp ../goview/go-view-serve-master/sqllite/goview.db ./web-screen/sqllite/goview.db
if [ -d ../goview/upload ]; then
	cp -r ../goview/upload/. ./web-screen/upload/
fi
find ./web-screen/html/dist -type f -name "*.js" -exec sed -i \
	-e 's#http://127.0.0.1:8083/api/goview#/api/goview#g' \
	-e 's#http://localhost:8083/api/goview#/api/goview#g' {} \;
