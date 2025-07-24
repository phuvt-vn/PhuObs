# Stage 1: Build the application using Maven
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package

# Stage 2: Deploy the application on a Tomcat server
FROM tomcat:10.1-jdk17-temurin

RUN mkdir -p /usr/local/tomcat/logs/gc
RUN mkdir -p /usr/local/tomcat/logs/heapdump

ENV CATALINA_OPTS="-Xms512m -Xmx1024m \
                   -javaagent:/usr/local/tomcat/lib/jmx_prometheus_javaagent-0.12.0.jar=9000:/usr/local/tomcat/conf/config.yaml \
                   -Xlog:gc*=warning:file=/usr/local/tomcat/logs/gc/gc.log:time,level,tags \
                   -XX:+HeapDumpOnOutOfMemoryError \
                   -XX:HeapDumpPath=/usr/local/tomcat/logs/heapdump/heapdump.hprof \
                   -Dcom.sun.management.jmxremote \
                   -Dcom.sun.management.jmxremote.port=9010 \
                   -Dcom.sun.management.jmxremote.rmi.port=9010 \
                   -Djava.rmi.server.hostname=0.0.0.0 \
                   -Dcom.sun.management.jmxremote.authenticate=false \
                   -Dcom.sun.management.jmxremote.ssl=false \
                   -DSMART_HOME=/data/SMART_HOME \
                   -Dfile.encoding=UTF-8 \
                   -Dfile.client.encoding=UTF-8 \
                   -Dclient.encoding.override=UTF-8"

COPY --from=build /app/target/ROOT.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080 9000 9010