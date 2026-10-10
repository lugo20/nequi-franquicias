locals {
  name = "${var.project}-${var.env}"

  tags = {
    project    = var.project
    env        = var.env
    component  = "franquiciasApiGateway"
    managed-by = "terraform"
  }
}
