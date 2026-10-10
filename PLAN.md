# Plan de trabajo - API de Franquicias

Plan de implementación de la prueba técnica Nequi: API para gestionar franquicias, sus sucursales y los productos de cada sucursal.

**Estado:** implementado, probado y desplegado en AWS. Las secciones de ramas reflejan el historial real de git, incluidos los ajustes de diseño hechos durante el desarrollo.

## Decisiones

| Tema | Decisión |
|---|---|
| Repositorio | Monorepo: `app/` (microservicio) + `iac/` (Terraform) |
| Flujo git | Git flow: `feature/*` desde `develop` → PR → `develop`; `release/x.y.z` → `main` + tag |
| Commits | Conventional commits en español; código en inglés |
| Aplicación | Spring Boot 3.5, Java 21, WebFlux (reactivo), clean architecture |
| Persistencia | DynamoDB on-demand, diseño single-table con claves `franchiseKey` / `entityKey` |
| API | Acción en la URL (`/create`, `/update-name`…), solo GET, POST y DELETE; POST recibe todo en el body; códigos HTTP; errores `{code, message}` |
| Local | docker-compose con DynamoDB Local; Dockerfile multi-stage |
| Nube | AWS us-east-1: API Gateway HTTP (URL fija, HTTPS) → VPC Link → ALB interno → ECS Fargate |
| Red | VPC nueva, tasks en subnets públicas sin NAT; ALB interno solo accesible desde el VPC Link; SG de tasks solo acepta tráfico del ALB |
| IaC | Terraform con `terraform-aws-modules`; solo ambiente `dev` |
| State Terraform | Bucket S3 versionado y cifrado, lock con `use_lockfile` |
| Pruebas | Cada commit incluye sus pruebas; el build falla si la cobertura de líneas baja del 80 % (Jacoco) |
| CI/CD | Sin pipeline; despliegue manual documentado |

### Reglas de negocio

- Nombre obligatorio en franquicias, sucursales y productos (400 `INVALID_NAME`); se recortan los espacios al inicio y al final.
- Stock obligatorio, entero y `>= 0` (400 `INVALID_STOCK`; un decimal o un texto dan 400 `INVALID_REQUEST`).
- Los ids que pide cada operación son obligatorios (400 `INVALID_REQUEST`).
- Nombre único entre sucursales de una franquicia y entre productos de una sucursal (409), sin distinguir mayúsculas.
- El nombre de la franquicia es único en todo el sistema (409), sin distinguir mayúsculas; se garantiza con un ítem de reserva de nombre escrito en la misma transacción que la franquicia. Su identidad es un UUID.
- Recurso inexistente (404). Una sucursal de otra franquicia o un producto de otra sucursal también dan 404.
- Al renombrar, el nombre se compara con los hermanos excluyéndose a sí mismo (se puede cambiar solo las mayúsculas).
- Producto con más stock: uno por sucursal; en empate, el primero por nombre; sucursales sin productos se omiten; resultado ordenado por nombre de sucursal.

### Modelo DynamoDB (single-table)

Claves: `franchiseKey` (partición) y `entityKey` (ordenamiento).

| Ítem | franchiseKey | entityKey |
|---|---|---|
| Franquicia | `FRANCHISE#<fid>` | `FRANCHISE` |
| Sucursal | `FRANCHISE#<fid>` | `BRANCH#<bid>` |
| Producto | `FRANCHISE#<fid>` | `BRANCH#<bid>#PRODUCT#<pid>` |
| Reserva de nombre de franquicia | `FRANCHISE_NAME#<nombre en minúsculas>` | `FRANCHISE_NAME` |

Un `Query` por `franchiseKey` trae la franquicia completa. La reserva de nombre guarda el `franchiseId` dueño del nombre.

## Endpoints (`/api/v1`)

POST lleva todos los parámetros en el body; GET y DELETE, en la URL. No hay endpoints de consulta o listado: el consumidor guarda los ids que devuelve cada `create`.

| Método | Ruta | Parámetros | Respuesta |
|---|---|---|---|
| POST | `/franchises/create` | `{name}` | 201 `{id}` |
| POST | `/franchises/update-name` | `{franchiseId, name}` | 200 `{id, name}` |
| GET | `/franchises/{franchiseId}/get-top-stock` | URL | 200 lista |
| POST | `/branches/create` | `{franchiseId, name}` | 201 `{id}` |
| POST | `/branches/update-name` | `{franchiseId, branchId, name}` | 200 `{id, name}` |
| POST | `/products/create` | `{franchiseId, branchId, name, stock}` | 201 `{id}` |
| POST | `/products/update-stock` | `{franchiseId, branchId, productId, stock}` | 200 `{id, name, stock}` |
| POST | `/products/update-name` | `{franchiseId, branchId, productId, name}` | 200 `{id, name, stock}` |
| DELETE | `/products/{franchiseId}/{branchId}/{productId}/delete` | URL | 204 |

## Estructura

```
nequi-franquicias/
├── app/
│   ├── applications/app-service/
│   ├── domain/model/
│   ├── domain/usecase/
│   ├── infrastructure/driven-adapters/dynamo-db/
│   ├── infrastructure/entry-points/reactive-web/
│   └── deployment/Dockerfile
├── iac/
│   ├── bootstrap/
│   ├── franquiciasDynamo/
│   ├── franquiciasEcr/
│   ├── franquiciasApiGateway/
│   ├── transversal/
│   └── ecsFranquicias/
├── docs/postman/
├── docker-compose.yml
├── PLAN.md
└── README.md
```

Cada componente de `iac/` contiene `providers.tf`, `inputs.tf`, `locals.tf`, `main.tf`, `outputs.tf` y `env/dev/terraform-dev.tfvars`; los que usan state remoto agregan `env/dev/backend-dev.hcl`, y los que leen salidas de otros componentes, `data.tf`.

## Ramas y commits

El README se actualiza en la misma rama que introduce cada funcionalidad, para que la documentación nunca quede desactualizada.

Cada commit incluye las pruebas de su código: el build exige 80 % de cobertura de líneas, así que todos los commits del historial compilan y pasan.

Las ramas de la fase C se trabajaron encadenadas (cada una sale de la anterior) y se integraron en orden.

### Fase 0 - Plan

- `feature/plan-trabajo` (PR #3)
  - `docs: agrega plan de trabajo`
  - `docs: agrega descripción inicial al README`

### Fase A - Base y primer endpoint local

- `feature/estructura-base` (PR #8)
  - `chore: agrega .gitignore y .gitattributes`
  - `build: agrega gradle wrapper y configuración raíz del proyecto`
  - `build: crea módulos model y usecase`
  - `build: crea módulos dynamo-db y reactive-web`
  - `feat: agrega app-service con MainApplication y application.yaml`
- `feature/modelo-dominio` (PR #9)
  - `feat(model): agrega entidades Franchise, Branch y Product`
  - `feat(model): agrega TechnicalMessage y excepciones de negocio`
  - `feat(model): agrega gateway FranchiseRepository`
- `feature/persistencia-dynamodb` (PR #10)
  - `feat(dynamo): configura cliente DynamoDB async con endpoint configurable`
  - `feat(dynamo): agrega entidad single-table con PK/SK`
  - `feat(dynamo): implementa guardado de franquicia, sucursal y producto`
  - `feat(dynamo): implementa consulta de franquicia por PK`
  - `feat(dynamo): implementa eliminación de producto y conecta el gateway`
  - `test(dynamo): agrega pruebas del adapter`
  - `build: agrega reporte de cobertura unificado y umbral mínimo`
- `feature/entorno-local` (PR #11)
  - `build: agrega Dockerfile multi-stage`
  - `chore: agrega docker-compose con DynamoDB Local y creación de tabla`
  - `docs: agrega ejecución local al README`
- `feature/manejo-errores` (PR #12)
  - `feat(api): agrega manejador global de errores HTTP`
  - `docs: ajusta plan a commits con pruebas incluidas`
- `feature/crear-franquicia` (PR #13): Incluye el nombre único de franquicia: se decidió al probar el primer endpoint.
  - `feat(usecase): agrega caso de uso crear franquicia`
  - `feat(api): expone POST /franchises`
  - `docs: documenta POST /franchises en el README`
  - `feat: agrega error FRANCHISE_NAME_DUPLICATED`
  - `feat(dynamo): reserva el nombre de franquicia con escritura transaccional`
  - `docs: actualiza reglas de nombre de franquicia en plan y README`

### Fase B - Walking skeleton en AWS

- `feature/iac-bootstrap` (PR #14)
  - `feat(iac): agrega bucket S3 para el state de Terraform`
- `feature/iac-dynamodb` (PR #15): El bucket del state se renombró para no exponer el id de la cuenta.
  - `feat(iac): agrega tabla DynamoDB on-demand`
  - `refactor(iac): nombra el bucket del state sin el id de la cuenta`
- `feature/ajusta-api-y-modelo` (PR #16): Rediseño tras probar el primer endpoint: acción en la URL (solo GET, POST y DELETE) y claves con nombres de negocio. La tabla de AWS se recreó vacía.
  - `refactor(dynamo): renombra claves a franchiseKey y entityKey`
  - `refactor(iac): renombra claves de la tabla DynamoDB`
  - `refactor(api): cambia la ruta de creación a /franchises/create`
  - `docs: actualiza plan y README con el diseño de endpoints y modelo`
- `feature/iac-ecr` (PR #17)
  - `feat(iac): agrega repositorio ECR`
- `feature/iac-transversal` (PR #18)
  - `feat(iac): agrega providers, inputs y tfvars de dev`
  - `feat(iac): agrega VPC con terraform-aws-modules/vpc`
  - `feat(iac): agrega ALB público y security groups`
  - `feat(iac): agrega cluster ECS`
- `feature/iac-ecs-servicio` (PR #19): Primer despliegue de punta a punta en AWS; luego se destruyeron servicio y red para no pagarlos mientras se desarrollaba.
  - `feat(iac): agrega rol IAM de la task con acceso a la tabla`
  - `feat(iac): agrega task definition y log group`
  - `feat(iac): agrega servicio ECS conectado al ALB`
  - `docs: agrega guía de despliegue en AWS al README`
  - `docs: parametriza la URL de la API en los ejemplos del README`
- `feature/iac-api-gateway` (PR #20): URL fija con HTTPS: API Gateway persistente y ALB interno, conectados por VPC Link.
  - `feat(iac): agrega API Gateway HTTP con stage por defecto`
  - `refactor(iac): convierte el ALB en interno y conecta API Gateway por VPC Link`
  - `refactor(iac): expone la URL de API Gateway como api_url`
  - `docs: documenta la arquitectura con API Gateway`
  - `docs: agrega la URL fija de la API al README`

### Fase C - Resto de endpoints

- `feature/crear-sucursal` (PR #21)
  - `feat(usecase): agrega caso de uso crear sucursal`
  - `feat(api): expone POST /branches/create`
  - `docs: corrige la respuesta de la URL con la infraestructura apagada`
  - `docs: documenta POST /branches/create en el README`
- `feature/crear-producto` (PR #22): Configura Jackson para rechazar un stock decimal en vez de truncarlo.
  - `feat(usecase): agrega caso de uso crear producto`
  - `feat(api): expone POST /products/create`
  - `docs: documenta POST /products/create en el README`
- `feature/eliminar-producto` (PR #23)
  - `feat(usecase): agrega caso de uso eliminar producto`
  - `feat(api): expone DELETE /products/{franchiseId}/{branchId}/{productId}/delete`
  - `docs: documenta DELETE de productos en el README`
- `feature/modificar-stock` (PR #24)
  - `feat(usecase): agrega caso de uso modificar stock`
  - `feat(api): expone POST /products/update-stock`
  - `docs: documenta POST /products/update-stock en el README`
- `feature/producto-max-stock` (PR #25)
  - `feat(usecase): agrega caso de uso producto con más stock por sucursal`
  - `feat(api): expone GET /franchises/{franchiseId}/get-top-stock`
  - `docs: documenta GET de producto con más stock en el README`
- `feature/renombrar-franquicia` (PR #26): Agrega `renameFranchise` al gateway: transacción que mueve la reserva del nombre.
  - `feat(dynamo): implementa renombrar franquicia moviendo la reserva del nombre`
  - `feat(usecase): agrega caso de uso renombrar franquicia`
  - `feat(api): expone POST /franchises/update-name`
  - `docs: documenta POST /franchises/update-name en el README`
- `feature/renombrar-sucursal` (PR #27)
  - `feat(usecase): agrega caso de uso renombrar sucursal`
  - `feat(api): expone POST /branches/update-name`
  - `docs: documenta POST /branches/update-name en el README`
- `feature/renombrar-producto` (PR #28)
  - `feat(usecase): agrega caso de uso renombrar producto`
  - `feat(api): expone POST /products/update-name`
  - `docs: documenta POST /products/update-name en el README`

### Fase D - Cierre

- `feature/corrige-ciclo-ecr`: la regla de ciclo de vida de ECR cuenta solo versiones con tag, y la imagen se publica sin metadatos de procedencia (una entrada por versión).
- `feature/documentacion`: colección de Postman y revisión final de README y plan.
- Despliegue final en AWS (imagen `:740d9ab`): los 9 endpoints validados por la URL fija.
- `release/1.0.0` → PR a `main` → tag `v1.0.0` → merge de vuelta a `develop`.
- Al terminar la evaluación: **destroy** total en orden `ecsFranquicias` → `transversal` → `franquiciasApiGateway` → `franquiciasEcr` → `franquiciasDynamo` → `bootstrap`.

## Orden de despliegue

1. `bootstrap` (state local)
2. `franquiciasDynamo`
3. `franquiciasEcr`
4. `franquiciasApiGateway`
5. `docker build` + `docker push` a ECR
6. `transversal`
7. `ecsFranquicias`

## Requisitos locales

- JDK 21
- Docker Desktop
- Terraform >= 1.10
- AWS CLI v2 con sesión iniciada (`aws login`, región `us-east-1`)
- Node.js (opcional, solo para ejecutar la colección de Postman con `npx newman`)
