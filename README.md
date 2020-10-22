# EVE Route

A route planner for [EVE Online](https://www.eveonline.com/) that supports Ansiblex jump gates and wormhole connections.

https://eve-route.herokuapp.com

<!-- toc -->

- [Setup](#setup)
  * [Git](#git)
  * [EVE App](#eve-app)
  * [Database](#database)
- [Build and Run](#build-and-run)
  * [Frontend](#frontend)
  * [Backend](#backend)
  * [Docker](#docker)
- [Deploy](#deploy)
  * [Heroku](#heroku)
- [Final Notes](#final-notes)
  * [Contact](#contact)
  * [Donations](#donations)
  * [Copyright Notice](#copyright-notice)

<!-- tocstop -->

## Setup

### Git

```shell script
git clone https://github.com/tkhamez/eve-route.git
cd eve-route
git submodule update --init
```

### EVE App

Create an EVE app at https://developers.eveonline.com with the following scopes
- esi-location.read_location.v1
- esi-search.search_structures.v1
- esi-universe.read_structures.v1
- esi-ui.write_waypoint.v1

Set the Callback URL to https://your.domain.tld/api/auth/login

### Database

The application needs a MongoDB, PostgreSQL, MySQL, MariaDB, SQLite or H2 (embedded mode) database.

## Build and Run

### Frontend

Requires [Node.js](https://nodejs.org/) 12 and [Yarn](https://yarnpkg.com/).

```shell script
cd frontend
yarn install
```

Dev: build map data and start server:
```shell script
npx ts-node src/scripts/map.ts
yarn start
```

Prod: build (files are copied to the backend into resources/public):
```shell script
yarn build
```

### Backend

Requires [JDK](https://openjdk.java.net/) 11.

Make sure the necessary environment variables are set, e.g.:
```shell script
export EVE_ROUTE_DB=mongodb://eve-route:password@127.0.0.1:27017/eve-route
export EVE_ROUTE_CLIENT_ID=ab12
export EVE_ROUTE_CLIENT_SECRET=12ab
export EVE_ROUTE_CALLBACK=http://localhost:8080/api/auth/login

# the following are optional (default values are defined in resources/application.conf):
export EVE_ROUTE_SECURE=1
export EVE_ROUTE_CORS_DOMAIN=localhost:3000
export EVE_ROUTE_ALLIANCE_ALLOWLIST=99003214,99010079
export EVE_ROUTE_OAUTH_AUTHORIZE=https://login.eveonline.com/v2/oauth/authorize
export EVE_ROUTE_OAUTH_TOKEN=https://login.eveonline.com/v2/oauth/token
export EVE_ROUTE_OAUTH_KEY_SET=https://login.eveonline.com/oauth/jwks
export EVE_ROUTE_OAUTH_ISSUER=login.eveonline.com
export EVE_ROUTE_ESI_DOMAIN=https://esi.evetech.net
export EVE_ROUTE_ESI_DATASOURCE=tranquility
```

Example connection strings for other databases: 
- jdbc:postgresql://user:pass@localhost:5432/db
- jdbc:mysql://user:pass@localhost/db?serverTimezone=UTC
- jdbc:mariadb://user:pass@localhost/db
- jdbc:sqlite:/data/data.db
- jdbc:h2:./h2file

The CORS domain setting includes http and https.

EVE_ROUTE_SECURE=1 enables the secure flag for the session cookie.

#### Generate Graph from ESI Data

Generate `resources/graph.json`:
```shell script
./gradlew buildGraph
```

#### Dev

Run the app:
```shell script
./gradlew run
```

To continuously rebuild on change, execute in a second console: 
```shell script
./gradlew build -t -x test -x shadowJar -x war
```

#### Tests

```shell script
./gradlew test
```

#### Debug

IntelliJ Configuration (from Kotlin template):
- Main class: io.ktor.server.netty.EngineMain
- Add environment variables
- Use classpath of module: eve-route.main

#### Fat JAR

```shell script
./gradlew shadowJar

java -jar build/libs/eve-route-0.3.1.jar
```

#### WAR (Servlet Container)

```shell script
./gradlew war

cd build/libs/ && jar -xvf eve-route-0.3.1.war
cd WEB-INF && java -classpath "lib/*:classes/." io.ktor.server.netty.EngineMain
```

### Docker

#### Development Environment

This was only tested so far on Linux with Docker 19.03 and Docker Compose 1.17.

```shell script
docker-compose up
```

This provides a MongoDB Server at port 27017, Mongo Express at http://localhost:8081, a container with
Gradle 6 and JDK 11 and one with Node.js 12 and Yarn.

Create shells to run commands for the frontend and backend:
```shell script
export UID && docker-compose run --service-ports node /bin/sh
export UID && docker-compose run --service-ports gradle /bin/bash

# second shell in the same grade container (adjust name, find name: $ docker ps)
docker exec -it everoute_gradle_run_1 /bin/bash
```

Set the necessary environment variables in the Gradle container:
```shell script
export EVE_ROUTE_CLIENT_ID=ab12
export EVE_ROUTE_CLIENT_SECRET=12ab
export EVE_ROUTE_CALLBACK=http://localhost:8080/api/auth/login
```

Note: Use `gradle` instead of `./gradlew`, this saves a download of ~100MB.

#### Production

Build the application jar file. e.g. in the Gradle container from the development environment:
```shell script
gradle shadowJar
```

Build the Docker container:
```shell script
docker build -t everoute .
```

Run the container, this example uses the Mongo database from docker-compose:
```shell script
docker run \
  --env EVE_ROUTE_DB=mongodb://eve-route:password@127.0.0.1:27017/eve-route \
  --env EVE_ROUTE_CLIENT_ID=ab12 \
  --env EVE_ROUTE_CLIENT_SECRET=12ab \
  --env EVE_ROUTE_CALLBACK=http://localhost:8080/api/auth/login \
  --network host -m512M --cpus 2 -it -p 8080:8080 --rm everoute
```

See also https://ktor.io/docs/docker.html.

## Deploy

### Heroku

Add build packs in this order:

```shell script
heroku buildpacks:add heroku/nodejs
heroku buildpacks:add heroku/gradle
```

## Final Notes

### Contact

If you have questions or feedback, you can join the EVE Route [Discord Server](https://discord.gg/EjzHx8p) 
or contact me via [Tweetfleet Slack](https://tweetfleet.slack.com) @Tian 
([invitations](https://slack.eveisesi.space/)).

### Donations

If you like this application, you can thank me by sending ISK to the character 
[Tian Khamez](https://evewho.com/character/96061222).

### Copyright Notice

EVE Route is licensed under the [MIT license](LICENSE).

"EVE", "EVE Online", "CCP" and all related logos and images are trademarks or registered trademarks of
[CCP hf](http://www.ccpgames.com/).
