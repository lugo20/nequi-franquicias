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

| Entorno | URL base |
|---|---|
| **AWS** | **`https://ebnrykpht7.execute-api.us-east-1.amazonaws.com/api/v1`** |
| Local | `http://localhost:8080/api/v1` |

La URL de AWS es fija (API Gateway). Si la infraestructura está apagada para ahorrar costos, responde `500 Internal Server Error` hasta que se vuelva a desplegar.

Los ejemplos usan la variable `API` con la URL base:

```bash
# AWS
API=https://ebnrykpht7.execute-api.us-east-1.amazonaws.com/api/v1

# Local (docker compose o bootRun)
API=http://localhost:8080/api/v1
```

En PowerShell: `$API = "https://ebnrykpht7.execute-api.us-east-1.amazonaws.com/api/v1"`.

Si se despliega en otra cuenta de AWS, la URL será distinta: se obtiene con `terraform -chdir=iac/ecsFranquicias output -raw api_url`.

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
curl -X POST $API/franchises/create -H "Content-Type: application/json" -d '{"name": "Cafe Express"}'
```

Respuesta `201 Created`:

```json
{ "id": "475b86b9-bd05-409c-8a61-1753882b4c5c" }
```

Errores: `400 INVALID_NAME`, `400 INVALID_REQUEST`, `409 FRANCHISE_NAME_DUPLICATED`.

### Crear sucursal

`POST /branches/create`

```bash
curl -X POST $API/branches/create -H "Content-Type: application/json" -d '{"franchiseId": "<franchiseId>", "name": "Norte"}'
```

Respuesta `201 Created`:

```json
{ "id": "f31d30f1-adad-4b9c-a127-a59af0407d44" }
```

Errores: `400 INVALID_REQUEST` (sin `franchiseId` o JSON inválido), `400 INVALID_NAME`, `404 FRANCHISE_NOT_FOUND`, `409 BRANCH_NAME_DUPLICATED`.

### Crear producto

`POST /products/create`

```bash
curl -X POST $API/products/create -H "Content-Type: application/json" -d '{"franchiseId": "<franchiseId>", "branchId": "<branchId>", "name": "Cafe", "stock": 10}'
```

Respuesta `201 Created`:

```json
{ "id": "6cc9e8e9-2ef7-4d3f-8512-9143a9d810d7" }
```

El `stock` debe ser un entero mayor o igual a 0. Errores: `400 INVALID_REQUEST` (sin ids, JSON inválido o stock no entero), `400 INVALID_NAME`, `400 INVALID_STOCK`, `404 FRANCHISE_NOT_FOUND`, `404 BRANCH_NOT_FOUND` (también si la sucursal no pertenece a esa franquicia), `409 PRODUCT_NAME_DUPLICATED`.

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

## Despliegue en AWS

La infraestructura se crea con Terraform en `us-east-1`. Todos los comandos se ejecutan desde la máquina local.

### Arquitectura

```
Cliente
   │  HTTPS (URL fija)
   ▼
API Gateway (HTTP API)              franquiciasApiGateway: nunca se destruye
   │  ruta ANY /{proxy+}
   ▼
VPC Link (privado)                  ┐
   ▼                                │ transversal
ALB interno :80                     │ (sin acceso desde internet)
   ▼                                ┘
Tarea ECS Fargate :8080             ecsFranquicias
   │  rol IAM de la tarea
   ▼
DynamoDB (nequi-franquicias-dev)    franquiciasDynamo
```

- **Única entrada pública: API Gateway**, con HTTPS. El ALB es interno y solo acepta tráfico del VPC Link; las tareas solo aceptan tráfico del ALB.
- **La URL es fija** (`https://<id>.execute-api.us-east-1.amazonaws.com`): la API vive en un componente que no se destruye. La ruta y la conexión al ALB se crean y destruyen junto con la red, así que al volver a encender la infraestructura la URL sigue siendo la misma. Mientras la red está apagada, la URL responde `500 Internal Server Error`: API Gateway no puede desplegar una API sin rutas y conserva su último despliegue, que apunta al VPC Link eliminado. Al volver a aplicar `transversal` se recupera sola.
- Las tareas usan IP pública solo para salir a internet (descargar la imagen, DynamoDB, CloudWatch): no hay NAT Gateway.

### Requisitos

- Terraform >= 1.10, AWS CLI v2 y Docker
- Sesión de AWS con permisos de administrador: `aws login` (o `aws configure`) y región `us-east-1`
- Verificar con `aws sts get-caller-identity`

### Componentes

Cada carpeta de `iac/` es un componente independiente con su propio state. El state de todos, salvo `bootstrap`, se guarda en el bucket S3 que crea `bootstrap`.

| Componente | Crea |
|---|---|
| `bootstrap` | Bucket S3 para el state de Terraform (versionado y cifrado) |
| `franquiciasDynamo` | Tabla DynamoDB on-demand |
| `franquiciasEcr` | Repositorio de imágenes (tags inmutables, escaneo, conserva las últimas 5) |
| `franquiciasApiGateway` | API Gateway HTTP con la URL pública fija (persistente) |
| `transversal` | VPC con 2 subnets públicas, ALB interno, VPC Link, ruta de API Gateway y cluster ECS |
| `ecsFranquicias` | Roles IAM, log group, task definition y servicio Fargate detrás del ALB |

### Primer despliegue

**1. Bucket del state** (una sola vez; su state queda local en `iac/bootstrap/terraform.tfstate`, no lo borres):

```bash
cd iac/bootstrap
terraform init
terraform apply -var-file=env/dev/terraform-dev.tfvars
```

**2. Tabla, repositorio de imágenes y API Gateway:**

```bash
cd iac/franquiciasDynamo
terraform init -backend-config=env/dev/backend-dev.hcl
terraform apply -var-file=env/dev/terraform-dev.tfvars

cd ../franquiciasEcr
terraform init -backend-config=env/dev/backend-dev.hcl
terraform apply -var-file=env/dev/terraform-dev.tfvars

cd ../franquiciasApiGateway
terraform init -backend-config=env/dev/backend-dev.hcl
terraform apply -var-file=env/dev/terraform-dev.tfvars
```

**3. Construir y publicar la imagen.** El tag es el hash corto del commit:

```bash
TAG=$(git rev-parse --short HEAD)
REPO=$(terraform -chdir=iac/franquiciasEcr output -raw repository_url)
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${REPO%%/*}
docker build -f app/deployment/Dockerfile -t $REPO:$TAG app
docker push $REPO:$TAG
```

> **Windows con Docker Desktop:** si `docker login` falla con `The stub received bad data`, es porque el token de ECR excede el tamaño que admite el almacén de credenciales de Windows. Se puede publicar con una configuración temporal de Docker:
>
> ```bash
> export DOCKER_CONFIG=$(mktemp -d)
> printf '{"auths":{"%s":{"auth":"%s"}}}' "${REPO%%/*}" "$(printf 'AWS:%s' "$(aws ecr get-login-password --region us-east-1)" | base64 -w0)" > "$DOCKER_CONFIG/config.json"
> docker push $REPO:$TAG
> rm -rf "$DOCKER_CONFIG"; unset DOCKER_CONFIG
> ```

**4. Red, balanceador y servicio:**

```bash
cd iac/transversal
terraform init -backend-config=env/dev/backend-dev.hcl
terraform apply -var-file=env/dev/terraform-dev.tfvars

cd ../ecsFranquicias
terraform init -backend-config=env/dev/backend-dev.hcl
terraform apply -var-file=env/dev/terraform-dev.tfvars -var="image_tag=$TAG"
```

El `apply` de `transversal` tarda unos 3 minutos (el VPC Link es lo más lento) y el del servicio espera a que la tarea esté sana (alrededor de un minuto). Al terminar muestra `api_url`. Si las primeras peticiones responden `503 Service Unavailable`, es la ruta de API Gateway terminando de propagarse: basta con esperar un minuto.

**5. Probar:**

```bash
API=$(terraform -chdir=iac/ecsFranquicias output -raw api_url)
curl -X POST $API/franchises/create -H "Content-Type: application/json" -d '{"name": "Cafe Express"}'
```

### Publicar una nueva versión

Repetir el paso 3 con el código nuevo (nuevo commit, nuevo tag) y luego:

```bash
terraform -chdir=iac/ecsFranquicias apply -var-file=env/dev/terraform-dev.tfvars -var="image_tag=$TAG"
```

ECS reemplaza la tarea sin cortar el servicio. Si la nueva versión no queda sana, vuelve sola a la anterior (*circuit breaker* con rollback).

### Logs

```bash
aws logs tail /ecs/nequi-franquicias-dev --follow
```

En Git Bash, anteponer `MSYS_NO_PATHCONV=1` para que no convierta `/ecs/...` en una ruta de Windows.

### Costos y limpieza

Con todo desplegado el costo es de unos **USD 1,30 al día**, casi todo del ALB y de la tarea Fargate. API Gateway cobra solo por petición, y DynamoDB, ECR y S3 cuestan centavos. Para no pagar mientras no se usa, se pueden destruir el servicio y la red y volver a aplicarlos después; **la URL de la API no cambia**:

```bash
terraform -chdir=iac/ecsFranquicias destroy -var-file=env/dev/terraform-dev.tfvars -var="image_tag=$TAG"
terraform -chdir=iac/transversal destroy -var-file=env/dev/terraform-dev.tfvars
```

Para eliminar todo, destruir en orden inverso: `ecsFranquicias` → `transversal` → `franquiciasApiGateway` → `franquiciasEcr` → `franquiciasDynamo` → `bootstrap`.

## Pruebas y cobertura

```bash
cd app
./gradlew build
```

Ejecuta las pruebas unitarias, genera el reporte de cobertura unificado y falla si la cobertura de líneas es menor al 80 %.

Reporte HTML: `app/build/reports/jacocoMergedReport/html/index.html`
