# EVE Route

A route planner for [EVE Online](https://www.eveonline.com/) that supports Ansiblex jump gates and wormhole connections.

Demo: https://eve-route.tian-space.net  
Docker: https://hub.docker.com/r/tkhamez/eve-route

![Screenshot](EVE-Route.png)

<!-- toc -->

- [Prerequisites](#prerequisites)
  * [EVE Application](#eve-application)
  * [Database](#database)
  * [Configuration with Environment Variables](#configuration-with-environment-variables)
- [Run the application](#run-the-application)
  * [JAR file](#jar-file)
  * [Docker Image](#docker-image)
- [Development Environment](#development-environment)
  * [Git](#git)
  * [Docker](#docker)
  * [Frontend](#frontend)
  * [Backend](#backend)
  * [Build Distribution](#build-distribution)
- [Final Notes](#final-notes)
  * [Contact](#contact)
  * [Donations](#donations)
  * [Copyright Notice](#copyright-notice)

<!-- tocstop -->

## Prerequisites

### EVE Application

Create an EVE application at https://developers.eveonline.com with the following scopes
- esi-location.read_location.v1
- esi-search.search_structures.v1
- esi-universe.read_structures.v1
- esi-ui.write_waypoint.v1

Set the Callback URL to https://your.domain.tld/api/auth/login

### Database

The application needs a MongoDB, PostgreSQL, MySQL, MariaDB, SQLite or H2 (embedded mode) database.

### Configuration with Environment Variables

The application is configured with environment variables. You can simply set them with `export VAR=value` for the
development environment or add them to your .bashrc file for example.

The following variables are required:
- EVE_ROUTE_DB - The database connection string.
- EVE_ROUTE_CLIENT_ID - Your EVE application client ID.
- EVE_ROUTE_CLIENT_SECRET - Your EVE application secret.
- EVE_ROUTE_CALLBACK - The callback URL from your EVE application.

Example connection strings for EVE_ROUTE_DB:
- mongodb://eve-route:password@localhost:27017/eve-route
- jdbc:postgresql://user:pass@localhost:5432/db
- jdbc:mysql://user:pass@localhost/db?serverTimezone=UTC
- jdbc:mariadb://user:pass@localhost/db
- jdbc:sqlite:/data/data.db
- jdbc:h2:./h2file

The following variables are optional, see resources/application.conf for their default values:
- PORT - The HTTP port for the webserver.
- EVE_ROUTE_OAUTH_AUTHORIZE
- EVE_ROUTE_OAUTH_TOKEN
- EVE_ROUTE_OAUTH_KEY_SET
- EVE_ROUTE_OAUTH_ISSUER
- EVE_ROUTE_ESI_DOMAIN
- EVE_ROUTE_ESI_DATASOURCE
- EVE_ROUTE_SECURE - Value 1 enables the secure flag for the session cookie.
- EVE_ROUTE_CORS_DOMAIN - Set this to the domain of the frontend if it is running on a different domain or port than
  the backend, it includes http and https.
- EVE_ROUTE_ALLIANCE_ALLOWLIST - A comma separated list of EVE alliances to restrict the login to their members.
- EVE_ROUTE_ROLE_IMPORT - A comma separated list of character IDs that can manually import Ansiblex gates.

## Run the application

### JAR file

This needs a Java 11 runtime.

You can download the fat JAR file from a [GitHub Release](https://github.com/tkhamez/eve-route/releases)
or build it yourself (see [Development Environment](#development-environment)).

Run the application - adjust the values to match your environment:
```shell script
java \
  -DEVE_ROUTE_DB=jdbc:sqlite:$PWD/sqlite.db \
  -DEVE_ROUTE_CLIENT_ID=ab12 \
  -DEVE_ROUTE_CLIENT_SECRET=12ab \
  -DEVE_ROUTE_CALLBACK=http://localhost:8080/api/auth/login \
  -jar eve-route.jar
```

### Docker Image

This need [Docker](https://www.docker.com/).

The image is available at https://hub.docker.com/r/tkhamez/eve-route.

Run the application, for example with a SQLite DB:
```shell script
docker run \
  --env EVE_ROUTE_DB=jdbc:sqlite:/data/sqlite.db \
  --env EVE_ROUTE_CLIENT_ID=ab12 \
  --env EVE_ROUTE_CLIENT_SECRET=12ab \
  --env EVE_ROUTE_CALLBACK=http://localhost:8080/api/auth/login \
  --mount type=bind,source="$(pwd)",target=/data \
  -p 8080:8080 --rm tkhamez/eve-route:1.0.0
```

You can also build the image yourself. For this make sure there is exactly one fat JAR file at 
`./build/libs/eve-route-*.jar`, then execute:
```shell script
docker build -t eve-route .
```

Run it, for example with the MongoDB from the Docker development environment:
```shell script
docker run \
  --env EVE_ROUTE_DB=mongodb://eve-route:password@localhost:27017/eve-route \
  --env EVE_ROUTE_CLIENT_ID=ab12 \
  --env EVE_ROUTE_CLIENT_SECRET=12ab \
  --env EVE_ROUTE_CALLBACK=http://localhost:8080/api/auth/login \
  --network host \
  --rm eve-route
```

## Development Environment

### Git

```shell script
git clone https://github.com/tkhamez/eve-route.git
cd eve-route
git submodule update --init
```

### Docker

This has only been tested on Linux with Docker 19.03 and [Docker Compose](https://docs.docker.com/compose/) 1.

Run the containers:
```shell script
export UID && docker-compose up
```

This provides a MongoDB Server at port 27017, a container with Gradle 6 and JDK 11 and one with Node.js 12 and Yarn.

Create shells to run commands for the frontend and backend:
```shell script
export UID && docker-compose run --service-ports node /bin/sh
export UID && docker-compose run --service-ports gradle /bin/bash

# second shell in the same Grade container (adjust name, find name: $ docker ps)
docker exec -it eve-route_gradle_run_5b2f82b4f035 /bin/bash
```

Note: You can use `gradle` instead of `./gradlew` to save ~100 MB download.

### Frontend

Requires [Node.js](https://nodejs.org/) 12 and [Yarn](https://yarnpkg.com/) 1.

Install dependencies:
```shell script
cd frontend
yarn install
```

Build map data and start the development server:
```shell script
npx ts-node src/scripts/map.ts
yarn start
```

### Backend

Requires [JDK](https://openjdk.java.net/) 11.

Generate `resources/graph.json` from ESI data:
```shell script
./gradlew buildGraph
```

Set the environment variables for the database and your EVE application, then run the app:
```shell script
./gradlew run
```

To continuously rebuild on change, execute in a second console: 
```shell script
./gradlew build -t -x test -x shadowJar -x war
```

Tests (needs environment variables):
```shell script
./gradlew test
```

IntelliJ debug configuration (from Kotlin template):
- Main class: io.ktor.server.netty.EngineMain
- Add environment variables
- Use classpath of module: eve-route.main

### Build Distribution

Build the frontend first, the files are copied to the backend into resources/public:
```shell script
cd frontend && yarn install
yarn build
```

Build the fat JAR
```shell script
./gradlew buildGraph
./gradlew shadowJar
```

Build the WAR file
```shell script
./gradlew buildGraph
./gradlew war
```

Test the WAR file:
```shell script
cd build/libs/ && jar -xvf eve-route-*.war
cd WEB-INF && java -classpath "lib/*:classes/." io.ktor.server.netty.EngineMain
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
