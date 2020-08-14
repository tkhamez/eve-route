# eve-route

## EVE App

Create an EVE app at https://developers.eveonline.com with the following scopes
- esi-location.read_location.v1
- esi-search.search_structures.v1
- esi-universe.read_structures.v1
- esi-ui.write_waypoint.v1

## Database

The app needs a MongoDB database.

You can use the included docker-compose file to create a server and provide a web-based GUI:
```shell script
docker-compose up
```

GUI: http://localhost:8081

## Run

Make sure the necessary environment variables are set, e.g.:
```
export EVE_ROUTE_DB=mongodb://eve-route:password@127.0.0.1:27017/eve-route
export EVE_ROUTE_CLIENT_ID=ab12
export EVE_ROUTE_CLIENT_SECRET=12ab
export EVE_ROUTE_CALLBACK=http://localhost:8080/login
```

### Dev

Run the app:
```
./gradlew run
```

To continuously rebuild on change, execute in a second console: 
```
./gradlew build -t -x test -x shadowJar -x war
```

#### Debug

IntelliJ Configuration:
- Main class: io.ktor.server.netty.EngineMain
- Add environment variables
- Use classpath of module: eve-route.main

### Fat JAR

```
./gradlew shadowJar

java -jar build/libs/eve-route-0.0.1.jar
```

### WAR (Servlet Container)

```
./gradlew war

cd build/libs/ && jar -xvf eve-route-0.0.1.war
cd WEB-INF && java -classpath "lib/*:classes/." io.ktor.server.netty.EngineMain
```
