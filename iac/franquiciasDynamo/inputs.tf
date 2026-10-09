variable "region" {
  description = "AWS region where the table is created."
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
