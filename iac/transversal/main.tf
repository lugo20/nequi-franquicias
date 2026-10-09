################################################################################
# VPC - Public subnets only: tasks get a public IP to pull images, no NAT gateway
################################################################################

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 6.7"

  name = local.name
  cidr = var.vpc_cidr

  azs            = var.availability_zones
  public_subnets = var.public_subnet_cidrs

  enable_nat_gateway   = false
  enable_dns_hostnames = true
  enable_dns_support   = true
}

################################################################################
# ALB - Internal: only reachable through API Gateway (VPC Link), never from internet
################################################################################

module "alb" {
  source  = "terraform-aws-modules/alb/aws"
  version = "~> 10.5"

  name    = local.name
  vpc_id  = module.vpc.vpc_id
  subnets = module.vpc.public_subnets

  internal = true
  # Test project: allows a full cleanup with terraform destroy.
  enable_deletion_protection = false

  security_group_ingress_rules = {
    http_from_api_gateway = {
      from_port                    = 80
      to_port                      = 80
      ip_protocol                  = "tcp"
      referenced_security_group_id = aws_security_group.vpc_link.id
    }
  }
  security_group_egress_rules = {
    app = {
      from_port   = var.app_port
      to_port     = var.app_port
      ip_protocol = "tcp"
      cidr_ipv4   = module.vpc.vpc_cidr_block
    }
  }

  listeners = {
    http = {
      port     = 80
      protocol = "HTTP"
      forward = {
        target_group_key = "app"
      }
    }
  }

  target_groups = {
    app = {
      name                 = local.name
      protocol             = "HTTP"
      port                 = var.app_port
      target_type          = "ip"
      deregistration_delay = 30
      # ECS registers the task IPs itself.
      create_attachment = false

      health_check = {
        enabled             = true
        path                = var.health_check_path
        matcher             = "200"
        interval            = 15
        timeout             = 5
        healthy_threshold   = 2
        unhealthy_threshold = 3
      }
    }
  }
}

################################################################################
# Security group of the ECS tasks - only the ALB can reach the application port
################################################################################

resource "aws_security_group" "tasks" {
  name        = "${local.name}-tasks"
  description = "ECS tasks: app port only from the ALB"
  vpc_id      = module.vpc.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "tasks_from_alb" {
  security_group_id            = aws_security_group.tasks.id
  referenced_security_group_id = module.alb.security_group_id
  from_port                    = var.app_port
  to_port                      = var.app_port
  ip_protocol                  = "tcp"
}

# Outbound to the internet: pull the image from ECR and reach DynamoDB and CloudWatch Logs.
resource "aws_vpc_security_group_egress_rule" "tasks_all" {
  security_group_id = aws_security_group.tasks.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

################################################################################
# ECS - Fargate cluster for the application service
################################################################################

resource "aws_ecs_cluster" "this" {
  name = local.name

  setting {
    name  = "containerInsights"
    value = "disabled"
  }
}

resource "aws_ecs_cluster_capacity_providers" "this" {
  cluster_name       = aws_ecs_cluster.this.name
  capacity_providers = ["FARGATE"]

  default_capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
  }
}

################################################################################
# API Gateway - Route and private integration to the internal ALB.
# The API itself (fixed URL) lives in franquiciasApiGateway and is never destroyed.
################################################################################

resource "aws_security_group" "vpc_link" {
  name        = "${local.name}-vpc-link"
  description = "API Gateway VPC Link: outbound only to the ALB"
  vpc_id      = module.vpc.vpc_id
}

resource "aws_vpc_security_group_egress_rule" "vpc_link_to_alb" {
  security_group_id            = aws_security_group.vpc_link.id
  referenced_security_group_id = module.alb.security_group_id
  from_port                    = 80
  to_port                      = 80
  ip_protocol                  = "tcp"
}

resource "aws_apigatewayv2_vpc_link" "this" {
  name               = local.name
  subnet_ids         = module.vpc.public_subnets
  security_group_ids = [aws_security_group.vpc_link.id]
}

resource "aws_apigatewayv2_integration" "alb" {
  api_id             = local.api_gateway.api_id
  integration_type   = "HTTP_PROXY"
  integration_method = "ANY"
  integration_uri    = module.alb.listeners["http"].arn
  connection_type    = "VPC_LINK"
  connection_id      = aws_apigatewayv2_vpc_link.this.id
}

resource "aws_apigatewayv2_route" "proxy" {
  api_id    = local.api_gateway.api_id
  route_key = "ANY /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.alb.id}"
}
