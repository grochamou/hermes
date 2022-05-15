heroku apps:destroy --app gcm-hermes-server --confirm gcm-hermes-server
heroku apps:create --region eu gcm-hermes-server
heroku config:set --app gcm-hermes-server SPRING_PROFILES_ACTIVE=heroku
./mvnw heroku:deploy-war -f hermes-server/pom.xml -Pheroku
