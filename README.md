# nequi-franquicias

API reactiva en Spring Boot para gestionar franquicias, sus sucursales y los productos de cada sucursal, con persistencia en DynamoDB y despliegue en AWS (ECS Fargate) aprovisionado con Terraform.

> Proyecto en construcción: este README se actualiza a medida que se agregan funcionalidades.

Ver el [plan de trabajo](PLAN.md) para las decisiones de arquitectura, endpoints y orden de implementación.

## Tecnologías

- Java 21, Spring Boot 3.5 con WebFlux (reactivo)
- Clean Architecture (plugin `co.com.bancolombia.cleanArchitecture`)
- DynamoDB (AWS SDK v2 asíncrono), diseño single-table
- Gradle 8.14 (wrapper incluido)
- Docker y Docker Compose

## Estructura

```
app/                                        Microservicio
├── applications/app-service/               Arranque y configuración de Spring
├── domain/model/                           Entidades, gateways y errores
├── domain/usecase/                         Casos de uso
├── infrastructure/driven-adapters/dynamo-db/   Persistencia en DynamoDB
├── infrastructure/entry-points/reactive-web/   API REST
└── deployment/Dockerfile
docker-compose.yml                          Entorno local completo
```

## Ejecución local

### Requisitos

- Docker Desktop (o Docker Engine con Compose v2)
- JDK 21, solo si se quiere compilar o ejecutar fuera de Docker

### Opción 1: todo con Docker Compose (recomendada)

Desde la raíz del repositorio:

```bash
docker compose up --build
```

Esto levanta tres servicios, en orden:

| Servicio | Qué hace |
|---|---|
| `dynamodb` | DynamoDB Local en memoria, puerto `8000` |
| `dynamodb-init` | Crea la tabla `franquicias` (PK `pk`, SK `sk`) y termina |
| `app` | La API en el puerto `8080`, apuntando a DynamoDB Local |

Verificar que la API está arriba:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP",...}
```

Detener y limpiar:

```bash
docker compose down
```

> DynamoDB Local corre en memoria: los datos se pierden al detener los contenedores.

### Opción 2: la app con Gradle y DynamoDB en Docker

Útil para desarrollar sin reconstruir la imagen en cada cambio.

```bash
docker compose up -d dynamodb dynamodb-init
cd app
```

En bash (Linux, macOS, Git Bash):

```bash
AWS_ACCESS_KEY_ID=local AWS_SECRET_ACCESS_KEY=local DYNAMODB_ENDPOINT=http://localhost:8000 ./gradlew bootRun
```

En PowerShell:

```powershell
$env:AWS_ACCESS_KEY_ID="local"; $env:AWS_SECRET_ACCESS_KEY="local"; $env:DYNAMODB_ENDPOINT="http://localhost:8000"; ./gradlew bootRun
```

DynamoDB Local acepta cualquier credencial; los valores `local` solo cumplen el formato que exige el SDK de AWS.

### Variables de entorno

| Variable | Por defecto | Descripción |
|---|---|---|
| `AWS_REGION` | `us-east-1` | Región de AWS |
| `DYNAMODB_ENDPOINT` | vacío | Endpoint de DynamoDB. Vacío usa AWS; en local, `http://localhost:8000` |
| `DYNAMODB_TABLE_NAME` | `franquicias` | Nombre de la tabla |

## Pruebas y cobertura

```bash
cd app
./gradlew build
```

Ejecuta las pruebas unitarias, genera el reporte de cobertura unificado y falla si la cobertura de líneas es menor al 80 %.

Reporte HTML: `app/build/reports/jacocoMergedReport/html/index.html`
