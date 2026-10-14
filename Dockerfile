# Imagem de producao. Os testes nao rodam aqui porque precisam de um PostgreSQL;
# rode .\mvnw.cmd -B test localmente antes de publicar.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
COPY src src
RUN mvn -B -q package -DskipTests \
    && cp target/financeiro-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --destination /app

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 financeiro
WORKDIR /app
COPY --from=build /app ./
USER financeiro

# Ajustado para caber em 512 MB (plano gratuito do Render) e iniciar mais rapido com pouca CPU.
ENV SERVER_PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -Duser.timezone=America/Sao_Paulo"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
