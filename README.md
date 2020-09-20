# EVE Route

A route planner for [EVE Online](https://www.eveonline.com/) that supports Ansiblex jump gates and wormhole connections.

https://eve-route.herokuapp.com

<!-- toc -->

- [Setup](#setup)
  * [EVE App](#eve-app)
  * [Database](#database)
- [Build and Run](#build-and-run)
  * [Frontend](#frontend)
  * [Backend](#backend)
- [Deploy to Heroku](#deploy-to-heroku)
- [Contact](#contact)
- [Donations](#donations)
- [Copyright Notice](#copyright-notice)

<!-- tocstop -->

## Setup

### EVE App

Create an EVE app at https://developers.eveonline.com with the following scopes
- esi-location.read_location.v1
- esi-search.search_structures.v1
- esi-universe.read_structures.v1
- esi-ui.write_waypoint.v1

Set the Callback URL to https://your.domain.tld/api/auth/login

### Database

The app needs a MongoDB, PostgreSQL, MySQL, MariaDB, SQLite or H2 (embedded mode) database.

You can use the included docker-compose file to create a MongoDB server and provide a web-based GUI:
```shell script
docker-compose up
```

GUI: http://localhost:8081

## Build and Run

### Frontend

Requires [Node.js](https://nodejs.org/) 12 and [Yarn](https://yarnpkg.com/).

```shell script
cd frontend
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

Requires [JDK](https://openjdk.java.net/) 11+.

Make sure the necessary environment variables are set, e.g.:
```shell script
export EVE_ROUTE_DB=mongodb://eve-route:password@127.0.0.1:27017/eve-route
export EVE_ROUTE_CLIENT_ID=ab12
export EVE_ROUTE_CLIENT_SECRET=12ab
export EVE_ROUTE_CALLBACK=http://localhost:8080/api/auth/login

# the following are optional:
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
./gradlew buildGraph
./gradlew shadowJar

java -jar build/libs/eve-route-0.2.0.jar
```

#### WAR (Servlet Container)

```shell script
./gradlew war

cd build/libs/ && jar -xvf eve-route-0.2.0.war
cd WEB-INF && java -classpath "lib/*:classes/." io.ktor.server.netty.EngineMain
```

## Deploy to Heroku

Add build packs in this order:

```shell script
heroku buildpacks:add heroku/nodejs
heroku buildpacks:add heroku/gradle
```

## Contact

If you have questions or feedback, you can join the EVE Route [Discord Server](https://discord.gg/EjzHx8p) 
or contact me via [Tweetfleet Slack](https://tweetfleet.slack.com) @Tian 
([invitations](https://slack.eveisesi.space/)).

## Donations

If you like this application, you can thank me by sending ISK to the character 
[Tian Khamez](https://evewho.com/character/96061222).

## Copyright Notice

EVE Route is licensed under the [MIT license](LICENSE).

"EVE", "EVE Online", "CCP" and all related logos and images are trademarks or registered trademarks of
[CCP hf](http://www.ccpgames.com/).
