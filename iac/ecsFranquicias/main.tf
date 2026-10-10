################################################################################
# IAM - Execution role (ECS agent) and task role (application)
################################################################################

data "aws_iam_policy_document" "ecs_tasks_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

# Used by ECS to pull the image from ECR and write logs to CloudWatch.
resource "aws_iam_role" "execution" {
  name               = "${local.name}-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Used by the application: only the DynamoDB actions it performs, only on its table.
resource "aws_iam_role" "task" {
  name               = "${local.name}-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

data "aws_iam_policy_document" "table_access" {
  statement {
    actions = [
      "dynamodb:Query",
      "dynamodb:PutItem",
      "dynamodb:DeleteItem",
      "dynamodb:ConditionCheckItem",
    ]
    resources = [local.dynamo.table_arn]
  }
}

resource "aws_iam_role_policy" "table_access" {
  name   = "table-access"
  role   = aws_iam_role.task.id
  policy = data.aws_iam_policy_document.table_access.json
}

################################################################################
# Task definition - Application container and its logs
################################################################################

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${local.name}"
  retention_in_days = var.log_retention_days
}

resource "aws_ecs_task_definition" "app" {
  family                   = local.name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([{
    name      = var.project
    image     = "${local.ecr.repository_url}:${var.image_tag}"
    essential = true

    portMappings = [{
      containerPort = var.app_port
      protocol      = "tcp"
    }]

    # No DYNAMODB_ENDPOINT: the SDK uses AWS, and the task role provides the credentials.
    environment = [
      { name = "AWS_REGION", value = var.region },
      { name = "DYNAMODB_TABLE_NAME", value = local.dynamo.table_name },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.app.name
        awslogs-region        = var.region
        awslogs-stream-prefix = "app"
      }
    }
  }])
}

################################################################################
# Service - Keeps the tasks running and registered in the ALB target group
################################################################################

resource "aws_ecs_service" "app" {
  name            = local.name
  cluster         = local.transversal.ecs_cluster_name
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = var.desired_count

  capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
  }

  network_configuration {
    subnets         = local.transversal.public_subnet_ids
    security_groups = [local.transversal.tasks_security_group_id]
    # Public subnets without NAT: the public IP lets the task reach ECR, DynamoDB and CloudWatch.
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = local.transversal.target_group_arn
    container_name   = var.project
    container_port   = var.app_port
  }

  # Time for Spring Boot to start before the ALB health checks count.
  health_check_grace_period_seconds = 60

  # A deployment that never becomes healthy is rolled back to the previous version.
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  # terraform apply waits until the tasks are healthy, so a failed deploy fails the apply.
  wait_for_steady_state = true
}
