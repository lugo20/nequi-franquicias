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
