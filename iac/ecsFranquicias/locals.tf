locals {
  name = "${var.project}-${var.env}"

  transversal = data.terraform_remote_state.transversal.outputs
  dynamo      = data.terraform_remote_state.dynamo.outputs
  ecr         = data.terraform_remote_state.ecr.outputs
  api_gateway = data.terraform_remote_state.api_gateway.outputs

  tags = {
    project    = var.project
    env        = var.env
    component  = "ecsFranquicias"
    managed-by = "terraform"
  }
}
