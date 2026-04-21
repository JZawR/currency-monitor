FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Копируем JAR
COPY build/libs/currency-monitor.jar app.jar

EXPOSE 8089

# Запускаем с ожиданием MongoDB
ENTRYPOINT ["sh", "-c", "sleep 5 && java -jar app.jar"]