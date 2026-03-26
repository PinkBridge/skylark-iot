FROM openjdk:17
WORKDIR /app

# A1：宿主机先 mvn package，容器仅负责运行 jar（避免拉取 maven/build 镜像超时）
COPY target/*.jar /app/app.jar

EXPOSE 8089
ENTRYPOINT ["java","-jar","/app/app.jar"]

