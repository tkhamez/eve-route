# eve-route

## EVE App

Create an EVE app at https://developers.eveonline.com with the scopes `esi-location.read_location.v1` 
and `and esi-ui.write_waypoint.v1`.

## Run

### Dev

First set the values for the environment variables and `<application>` in `src/main/webapp/WEB-INF/appengine-web.xml`, 
(do *not* commit the changes) then run with:
```
./gradlew appengineRun
```

To continuously rebuild on change, execute in a second console:
```
./gradlew build -t -x test
```

#### Debug

IntelliJ Configuration:
- Main class: io.ktor.server.netty.EngineMain
- Environment Variables: EVE_ROUTE_CLIENT_ID, EVE_ROUTE_CLIENT_SECRET and EVE_ROUTE_CALLBACK
- Use classpath of module: eve-route.main

### Deploy to App Engine

See also https://ktor.io/servers/deploy/hosting/google-app-engine.html for prerequisite.

First, create project and app (change the name):
```
gcloud projects create eve-route --set-as-default
(change: gcloud config set project [project-id])
gcloud app create
```

Then setup Firestore
- Configure your project to use Cloud Firestore in Native mode. https://console.cloud.google.com/firestore
- Enable Cloud Firestore API inside your Google Cloud project. https://console.cloud.google.com/flows/enableapi?apiid=firestore.googleapis.com
- Set up authentication. https://cloud.google.com/docs/authentication/getting-started

Set the values for the environment variables and `<application>` in `src/main/webapp/WEB-INF/appengine-web.xml`
(do *not* commit the changes)

Deploy with:
```
./gradlew appengineDeploy
```

### Fat JAR

```
./gradlew shadow

export EVE_ROUTE_CLIENT_ID=123...
export EVE_ROUTE_CLIENT_SECRET=abc...
export EVE_ROUTE_CALLBACK=http://localhost:8080/login
java -jar build/libs/eve-route-0.0.1.jar
```

### WAR (Servlet Container)

```
./gradlew war

export EVE_ROUTE_CLIENT_ID=123...
export EVE_ROUTE_CLIENT_SECRET=abc...
export EVE_ROUTE_CALLBACK=http://localhost:8080/login
cd build/libs/ && jar -xvf eve-route-0.0.1.war
cd WEB-INF && java -classpath "lib/*:classes/." io.ktor.server.netty.EngineMain
```
