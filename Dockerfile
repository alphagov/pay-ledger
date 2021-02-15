FROM adoptopenjdk/openjdk11:jre-11.0.9_11.1-alpine@sha256:18a90fe4c1b4140ce960294edb05c9ab5113fd4868a2c06b885c33db3bf99ab3

RUN ["apk", "--no-cache", "upgrade"]

ARG DNS_TTL=15

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

RUN echo networkaddress.cache.ttl=$DNS_TTL >> "$JAVA_HOME/conf/security/java.security"

# Add RDS CA certificates to the default truststore
RUN wget -qO - https://s3.amazonaws.com/rds-downloads/rds-ca-2019-root.pem       | keytool -importcert -noprompt -cacerts -storepass changeit -alias rds-ca-2019-root \
 && wget -qO - https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem | keytool -importcert -noprompt -cacerts -storepass changeit -alias rds-combined-ca-bundle

RUN ["apk", "add", "--no-cache", "bash"]

ENV PORT 8080

EXPOSE 8080

WORKDIR /app

ADD docker-startup.sh /app/docker-startup.sh
ADD target/*.yaml /app/
ADD target/pay-*-allinone.jar /app/

CMD bash ./docker-startup.sh
