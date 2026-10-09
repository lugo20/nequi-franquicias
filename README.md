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
| `dynamodb-init` | Crea la tabla `franquicias` (claves `franchiseKey` y `entityKey`) y termina |
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

## API

Base: `http://localhost:8080/api/v1`

### Convenciones

- La URL indica la acción: `create`, `update-name`, `update-stock`, `get-top-stock`, `delete`.
- Solo se usan tres métodos: **GET** consulta, **POST** crea o actualiza, **DELETE** elimina.
- **POST** recibe todos sus parámetros en el body (JSON). **GET** y **DELETE** los reciben en la URL.
- Cada `create` responde con el `id` generado. **Guárdalo:** las demás operaciones lo piden, y la API no tiene endpoints para listar o buscar.
- Los nombres se guardan sin espacios al inicio ni al final; un nombre vacío o solo con espacios es inválido.
- El nombre de una franquicia es único en todo el sistema, sin distinguir mayúsculas: `Cafe Express` y `cafe express` son el mismo. Los nombres de sucursal son únicos dentro de su franquicia, y los de producto dentro de su sucursal.

### Errores

Responden con el status HTTP correspondiente y el cuerpo:

```json
{ "code": "INVALID_NAME", "message": "El nombre es obligatorio" }
```

| Status | Códigos |
|---|---|
| 400 | `INVALID_NAME`, `INVALID_STOCK`, `INVALID_REQUEST` (JSON mal formado o sin cuerpo) |
| 404 | `FRANCHISE_NOT_FOUND`, `BRANCH_NOT_FOUND`, `PRODUCT_NOT_FOUND` |
| 409 | `FRANCHISE_NAME_DUPLICATED`, `BRANCH_NAME_DUPLICATED`, `PRODUCT_NAME_DUPLICATED` |
| 500 | `TECHNICAL_ERROR` |

### Crear franquicia

`POST /franchises/create`

```bash
curl -X POST http://localhost:8080/api/v1/franchises/create -H "Content-Type: application/json" -d '{"name": "Cafe Express"}'
```

Respuesta `201 Created`:

```json
{ "id": "475b86b9-bd05-409c-8a61-1753882b4c5c" }
```

Errores: `400 INVALID_NAME`, `400 INVALID_REQUEST`, `409 FRANCHISE_NAME_DUPLICATED`.

## Modelo de datos

Todo se guarda en **una sola tabla de DynamoDB** (diseño *single-table*). Cada franquicia forma un **grupo**: la franquicia, sus sucursales y sus productos comparten la misma clave de partición, así que una sola consulta trae la franquicia completa.

### Columnas

| Columna | Descripción |
|---|---|
| `franchiseKey` | Clave de partición: **a qué franquicia pertenece** la fila (`FRANCHISE#<franchiseId>`). En las reservas de nombre es `FRANCHISE_NAME#<nombre en minúsculas>` |
| `entityKey` | Clave de ordenamiento: **qué es la fila** dentro de la franquicia (`FRANCHISE`, `BRANCH#<branchId>`, `BRANCH#<branchId>#PRODUCT#<productId>` o `FRANCHISE_NAME`) |
| `type` | Tipo de fila: `FRANCHISE`, `BRANCH`, `PRODUCT` o `FRANCHISE_NAME` |
| `id` | Id de la franquicia, sucursal o producto. En la reserva de nombre, el id de la franquicia dueña |
| `branchId` | Solo en productos: la sucursal a la que pertenecen |
| `name` | Nombre |
| `stock` | Solo en productos: cantidad en stock |

El `#` es solo un separador de texto. Como DynamoDB ordena las filas de un grupo por `entityKey`, cada sucursal queda seguida de sus productos.

### Ejemplo

Franquicia *Cafe Express* con las sucursales *Norte* (productos *Cafe* y *Te*) y *Sur* (producto *Pastel*):

| franchiseKey | entityKey | type | id | branchId | name | stock |
|---|---|---|---|---|---|---|
| `FRANCHISE#f1` | `BRANCH#b1` | BRANCH | b1 | | Norte | |
| `FRANCHISE#f1` | `BRANCH#b1#PRODUCT#p1` | PRODUCT | p1 | b1 | Cafe | 10 |
| `FRANCHISE#f1` | `BRANCH#b1#PRODUCT#p2` | PRODUCT | p2 | b1 | Te | 3 |
| `FRANCHISE#f1` | `BRANCH#b2` | BRANCH | b2 | | Sur | |
| `FRANCHISE#f1` | `BRANCH#b2#PRODUCT#p3` | PRODUCT | p3 | b2 | Pastel | 7 |
| `FRANCHISE#f1` | `FRANCHISE` | FRANCHISE | f1 | | Cafe Express | |
| `FRANCHISE_NAME#cafe express` | `FRANCHISE_NAME` | FRANCHISE_NAME | f1 | | Cafe Express | |

### Cómo se usa

| Operación | En la tabla |
|---|---|
| Leer una franquicia (para validar o calcular el top de stock) | Un `Query` por `franchiseKey = FRANCHISE#f1` |
| Crear o actualizar sucursal o producto | Escribe (o reemplaza) solo su fila |
| Eliminar producto | Borra solo su fila |
| Crear franquicia | **Transacción:** la fila de la franquicia más su reserva de nombre, con la condición de que la reserva no exista |
| Renombrar franquicia | **Transacción:** actualiza la franquicia, libera la reserva del nombre viejo y crea la del nuevo |

**Por qué existe la reserva de nombre:** DynamoDB solo garantiza unicidad sobre la clave de una fila. La reserva es una fila cuya clave **es** el nombre; al escribirla con la condición "solo si no existe" dentro de la misma transacción que la franquicia, dos franquicias nunca pueden tener el mismo nombre, ni siquiera si se crean al mismo tiempo.

## Pruebas y cobertura

```bash
cd app
./gradlew build
```

Ejecuta las pruebas unitarias, genera el reporte de cobertura unificado y falla si la cobertura de líneas es menor al 80 %.

Reporte HTML: `app/build/reports/jacocoMergedReport/html/index.html`
