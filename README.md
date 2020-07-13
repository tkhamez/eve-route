# eve-route

## EVE App

Create an EVE app at https://developers.eveonline.com with the scopes `esi-location.read_location.v1` 
and `and esi-ui.write_waypoint.v1`.

## Run

First set the values for the environment variables in `src/main/webapp/WEB-INF/appengine-web.xml`, 
(do *not* commit the changes) then run with:
```
./gradlew appengineRun
```

To continuously rebuild on change, execute in a second console:
```
./gradlew build -t -x test
```

## Deploy to App Engine

See also https://ktor.io/servers/deploy/hosting/google-app-engine.html

First, create project and app (change the name):
```
gcloud projects create eve-routes --set-as-default
gcloud app create
```

To deploy, set the values for the environment variables in `src/main/webapp/WEB-INF/appengine-web.xml` 
(do *not* commit the changes), then execute:
```
./gradlew appengineDeploy
```

## Fat JAR

```
./gradlew shadow
java -jar build/libs/eve-route-0.0.1.jar
```
