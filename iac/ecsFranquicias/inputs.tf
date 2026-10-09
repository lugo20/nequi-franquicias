variable "region" {
  description = "AWS region where the service runs."
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

variable "state_bucket" {
  description = "Bucket that holds the state of the other components."
  type        = string
}

variable "image_tag" {
  description = "Image tag to deploy (short hash of the commit). Passed on each deploy with -var."
  type        = string
}

variable "task_cpu" {
  description = "Task CPU units (1024 = 1 vCPU)."
  type        = number
}

variable "task_memory" {
  description = "Task memory in MiB."
  type        = number
}

variable "app_port" {
  description = "Port where the application container listens."
  type        = number
}

variable "log_retention_days" {
  description = "Days the application logs are kept in CloudWatch."
  type        = number
}

variable "desired_count" {
  description = "Number of tasks running."
  type        = number
}
