variable "region" {
  description = "AWS region where the network and load balancer are created."
  type        = string
}

variable "project" {
  description = "Project name, used in resource names and tags."
  type        = string
}

variable "env" {
  description = "Environment name."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block of the VPC."
  type        = string
}

variable "availability_zones" {
  description = "Availability zones used by the public subnets (the ALB needs at least two)."
  type        = list(string)
}

variable "public_subnet_cidrs" {
  description = "One public subnet CIDR per availability zone."
  type        = list(string)
}

variable "app_port" {
  description = "Port where the application container listens."
  type        = number
}

variable "health_check_path" {
  description = "Path the load balancer uses to check the application health."
  type        = string
}

variable "state_bucket" {
  description = "Bucket that holds the state of the other components."
  type        = string
}
