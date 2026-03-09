# swe-skeleton

SKEL: Skeleton Project
This project provides a starting point for development of projects during the
course "Software Engineering". It is a simple web application offering nearly
no "real" functionality. Its main purpose is to help you getting started quickly
by providing a suitable starting point.
It utilizes Spring Boot and is configured as a Maven web application project with:

This project works with Java 21.
Execute "mvn spring-boot:run" to start the skeleton project and install
required js libraries.
Change .env.example to .env (can be found in src/main/frontend) and check
that the URL is set to "localhost:8080" before starting the frontend.
Execute "npm start" in the folder src/main/frontend to start the frontend,
which you can access at http://localhost:3000/ ([see also frontend README](./src/main/frontend/))

This project integrates Springdoc OpenAPI with Swagger UI. You can access the generated API documentation
at: http://localhost:8080/swagger-ui.html.

Further, this project automatically generates the frontend API based on the OpenAPI specification. Generation is done
during `mvn clean install` or through `mvn generate-sources`. The generated API is placed at
`src/main/frontend/src/generated-skeleton-api` folder. The generated API is based on the OpenAPI specification located
in `src/main/resources/openapi.json`. Make sure to update this file to reflect the current API specification or modify
the `pom.xml` file to pull the latest one from the skeleton project via HTTP.

You can log in with:

- "admin" and "passwd"
- "user1" and "passwd"
- "user2" and "passwd"
- "elvis" and "passwd"

Feel free to use this skeleton project as you see fit - but keep in mind that
this project is primarily provided to be used for educational purposes. Don't
use it for production!

## Debugging React

This project contains run configurations to debug the frontend for both IntelliJ/WebStorm and Visual
Studio Code (see `.run` and `.vscode/launch.json` respectively). By default, it will launch the
`chrome` browser and start npm using `npm start`, but you can change this in the run configuration
if necessary (
see https://www.jetbrains.com/help/idea/run-debug-configuration-javascript-debug.html
and https://code.visualstudio.com/docs/typescript/typescript-debugging). After that, you can set
breakpoints in your React code and debug it as you would with Java code.
Further, we recommend installing the React Developer tools, available for firefox and chromium based
browsers (https://react.dev/learn/react-developer-tools).

## Contributors:

- Christian Sillaber
- Michael Brunner
- Clemens Sauerwein
- Andrea Mussmann
- Alexander Blaas
- Zoe Pfister
- Lorenz Brehme
