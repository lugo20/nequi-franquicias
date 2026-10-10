locals {
  table_name = "${var.project}-${var.env}"

  tags = {
    project    = var.project
    env        = var.env
    component  = "franquiciasDynamo"
    managed-by = "terraform"
  }
}
