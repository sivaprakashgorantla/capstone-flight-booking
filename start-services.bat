echo Starting Microservices...

start cmd /k "cd eureka-server && mvn spring-boot:run"
timeout /t 5

start cmd /k "cd api-gateway && mvn spring-boot:run"
timeout /t 5

start cmd /k "cd auth-service && mvn spring-boot:run"
start cmd /k "cd user-service && mvn spring-boot:run"
start cmd /k "cd flight-service && mvn spring-boot:run"
start cmd /k "cd booking-service && mvn spring-boot:run"
start cmd /k "cd payment-service && mvn spring-boot:run"
start cmd /k "cd cancellation-service && mvn spring-boot:run"
start cmd /k "cd support-service && mvn spring-boot:run"
start cmd /k "cd notification-service && mvn spring-boot:run"
start cmd /k "cd analytics-reporting-service && mvn spring-boot:run"

echo All services started