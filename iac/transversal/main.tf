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
# ALB - Public HTTP entry point that forwards every request to the application
################################################################################

module "alb" {
  source  = "terraform-aws-modules/alb/aws"
  version = "~> 10.5"

  name    = local.name
  vpc_id  = module.vpc.vpc_id
  subnets = module.vpc.public_subnets

  # Test project: allows a full cleanup with terraform destroy.
  enable_deletion_protection = false

  security_group_ingress_rules = {
    http = {
      from_port   = 80
      to_port     = 80
      ip_protocol = "tcp"
      cidr_ipv4   = "0.0.0.0/0"
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
