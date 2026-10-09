locals {
  name = "${var.project}-${var.env}"

  api_gateway = data.terraform_remote_state.api_gateway.outputs

  tags = {
    project    = var.project
    env        = var.env
    component  = "transversal"
    managed-by = "terraform"
  }
}
