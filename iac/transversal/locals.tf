locals {
  name = "${var.project}-${var.env}"

  tags = {
    project    = var.project
    env        = var.env
    component  = "transversal"
    managed-by = "terraform"
  }
}
