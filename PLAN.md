# Plan de trabajo - API de Franquicias

Plan de implementación de la prueba técnica Nequi: API para gestionar franquicias, sus sucursales y los productos de cada sucursal.

## Decisiones

| Tema | Decisión |
|---|---|
| Repositorio | Monorepo: `app/` (microservicio) + `iac/` (Terraform) |
| Flujo git | Git flow: `feature/*` desde `develop` → PR → `develop`; `release/x.y.z` → `main` + tag |
| Commits | Conventional commits en español; código en inglés |
| Aplicación | Spring Boot 3.5, Java 21, WebFlux (reactivo), clean architecture |
| Persistencia | DynamoDB, diseño single-table |
| API | REST plano con códigos HTTP; errores `{code, message}` |
| Local | docker-compose con DynamoDB Local; Dockerfile multi-stage |
| Nube | AWS us-east-1: ECS Fargate + ALB público HTTP |
| Red | VPC nueva, tasks en subnets públicas sin NAT; SG de tasks solo acepta tráfico del ALB |
| IaC | Terraform con `terraform-aws-modules`; solo ambiente `dev` |
| State Terraform | Bucket S3 versionado y cifrado, lock con `use_lockfile` |
| CI/CD | Sin pipeline; despliegue manual documentado |

### Reglas de negocio

- Nombre obligatorio en franquicias, sucursales y productos (400).
- Stock obligatorio y `>= 0` (400).
- Nombre único entre sucursales de una franquicia y entre productos de una sucursal (409).
- Las franquicias pueden repetir nombre; su identidad es un UUID.
- Recurso inexistente (404).
- Producto con más stock: uno por sucursal; en empate, el primero por nombre; sucursales sin productos se omiten.

### Modelo DynamoDB (single-table)

| Ítem | PK | SK |
|---|---|---|
| Franquicia | `FRANCHISE#<fid>` | `FRANCHISE` |
| Sucursal | `FRANCHISE#<fid>` | `BRANCH#<bid>` |
| Producto | `FRANCHISE#<fid>` | `BRANCH#<bid>#PRODUCT#<pid>` |

Un `Query` por PK trae la franquicia completa.

## Endpoints (`/api/v1`)

| Método | Ruta | Respuesta |
|---|---|---|
| POST | `/franchises` | 201 |
| PATCH | `/franchises/{fid}/name` | 200 |
| POST | `/franchises/{fid}/branches` | 201 |
| PATCH | `/franchises/{fid}/branches/{bid}/name` | 200 |
| POST | `/franchises/{fid}/branches/{bid}/products` | 201 |
| DELETE | `/franchises/{fid}/branches/{bid}/products/{pid}` | 204 |
| PATCH | `/franchises/{fid}/branches/{bid}/products/{pid}/stock` | 200 |
| PATCH | `/franchises/{fid}/branches/{bid}/products/{pid}/name` | 200 |
| GET | `/franchises/{fid}/products/top-stock` | 200 |

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
│   ├── transversal/
│   └── ecsFranquicias/
├── docker-compose.yml
├── PLAN.md
└── README.md
```

Cada componente de `iac/` contiene `main.tf`, `inputs.tf`, `data.tf`, `locals.tf`, `outputs.tf` y `env/dev/terraform-dev.tfvars`.

## Ramas y commits

### Fase A - Base y primer endpoint local

1. `feature/estructura-base`
   - `chore: agrega .gitignore y .gitattributes`
   - `build: agrega gradle wrapper y configuración raíz del proyecto`
   - `build: crea módulos model y usecase`
   - `build: crea módulos dynamo-db y reactive-web`
   - `feat: agrega app-service con MainApplication y application.yaml`
2. `feature/modelo-dominio`
   - `feat(model): agrega entidades Franchise, Branch y Product`
   - `feat(model): agrega TechnicalMessage y excepciones de negocio`
   - `feat(model): agrega gateway FranchiseRepository`
3. `feature/persistencia-dynamodb`
   - `feat(dynamo): configura cliente DynamoDB async con endpoint configurable`
   - `feat(dynamo): agrega entidad single-table con PK/SK`
   - `feat(dynamo): implementa guardado de franquicia, sucursal y producto`
   - `feat(dynamo): implementa consulta de franquicia por PK`
   - `feat(dynamo): implementa actualización y eliminación de items`
   - `test(dynamo): agrega pruebas del adapter`
4. `feature/entorno-local`
   - `build: agrega Dockerfile multi-stage`
   - `chore: agrega docker-compose con DynamoDB Local y creación de tabla`
5. `feature/manejo-errores`
   - `feat(api): agrega RouterRest base y manejador global de errores HTTP`
   - `test(api): agrega pruebas del manejador de errores`
6. `feature/crear-franquicia`
   - `feat(usecase): agrega caso de uso crear franquicia`
   - `feat(api): expone POST /franchises`
   - `test(api): agrega pruebas del handler`

### Fase B - Walking skeleton en AWS

7. `feature/iac-bootstrap`
   - `feat(iac): agrega bucket S3 para el state de Terraform`
8. `feature/iac-dynamodb`
   - `feat(iac): agrega tabla DynamoDB on-demand`
9. `feature/iac-ecr`
   - `feat(iac): agrega repositorio ECR`
10. `feature/iac-transversal`
    - `feat(iac): agrega providers, inputs y tfvars de dev`
    - `feat(iac): agrega VPC con terraform-aws-modules/vpc`
    - `feat(iac): agrega ALB público y security groups`
    - `feat(iac): agrega cluster ECS`
11. `feature/iac-ecs-servicio`
    - `feat(iac): agrega rol IAM de la task con acceso a la tabla`
    - `feat(iac): agrega task definition y log group`
    - `feat(iac): agrega servicio ECS conectado al ALB`
    - Validar `POST /franchises` vía URL del ALB.
    - **Destroy** de `ecsFranquicias` y `transversal` para no pagar ALB y Fargate mientras se desarrolla.

### Fase C - Resto de endpoints

Cada rama: `feat(usecase)` con su test → `feat(api)` handler y ruta → `test(api)` handler.

12. `feature/crear-sucursal`
13. `feature/crear-producto`
14. `feature/eliminar-producto`
15. `feature/modificar-stock`
16. `feature/producto-max-stock`
17. `feature/renombrar-franquicia`
18. `feature/renombrar-sucursal`
19. `feature/renombrar-producto`

### Fase D - Cierre

20. `feature/documentacion`
    - `docs: agrega README con ejecución local`
    - `docs: agrega guía de despliegue en AWS`
    - `docs: agrega colección de requests de ejemplo`
    - **Re-apply** de `transversal` y `ecsFranquicias` y push de la imagen final.
21. `release/1.0.0` → PR a `main` → tag `v1.0.0` → merge de vuelta a `develop`.
    - Al terminar la evaluación: **destroy** total en orden `ecsFranquicias` → `transversal` → `franquiciasEcr` → `franquiciasDynamo` → `bootstrap`.

## Orden de despliegue

1. `bootstrap` (state local)
2. `franquiciasDynamo`
3. `franquiciasEcr`
4. `docker build` + `docker push` a ECR
5. `transversal`
6. `ecsFranquicias`

## Requisitos locales

- JDK 21
- Docker Desktop
- Terraform >= 1.10
- AWS CLI v2 configurado (`aws configure`, región `us-east-1`)
