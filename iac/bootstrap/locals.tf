locals {
  bucket_name = "${var.project}-tfstate"

  tags = {
    project    = var.project
    env        = var.env
    component  = "bootstrap"
    managed-by = "terraform"
  }
}
