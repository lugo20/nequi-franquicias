variable "region" {
  description = "AWS region where the repository is created."
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

variable "images_to_keep" {
  description = "Number of most recent images kept in the repository."
  type        = number
}
