locals {
  repository_name = var.project

  tags = {
    project    = var.project
    env        = var.env
    component  = "franquiciasEcr"
    managed-by = "terraform"
  }
}
