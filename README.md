# eve-route

## EVE App

Create an EVE app at https://developers.eveonline.com with the scopes `esi-location.read_location.v1` 
and `and esi-ui.write_waypoint.v1`.

## Run

### Dev

Set the environment variables and run the app:
```
export EVE_ROUTE_CLIENT_ID=ab12
export EVE_ROUTE_CLIENT_SECRET=12ab
export EVE_ROUTE_CALLBACK=http://localhost:8080/login

./gradlew run
```

To continuously rebuild on change, execute in a second console: 
```
./gradlew build -t -x test -x shadowJar
```

#### Debug

IntelliJ Configuration:
- Main class: io.ktor.server.netty.EngineMain
- Environment Variables: EVE_ROUTE_CLIENT_ID, EVE_ROUTE_CLIENT_SECRET and EVE_ROUTE_CALLBACK
- Use classpath of module: eve-route.main

### Fat JAR

```
./gradlew shadowJar

export EVE_ROUTE_CLIENT_ID=123...
export EVE_ROUTE_CLIENT_SECRET=abc...
export EVE_ROUTE_CALLBACK=http://localhost:8080/login
java -jar build/libs/eve-route-0.0.1.jar
```
