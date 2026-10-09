locals {
  name = "${var.project}-${var.env}"

  transversal = data.terraform_remote_state.transversal.outputs
  dynamo      = data.terraform_remote_state.dynamo.outputs
  ecr         = data.terraform_remote_state.ecr.outputs

  tags = {
    project    = var.project
    env        = var.env
    component  = "ecsFranquicias"
    managed-by = "terraform"
  }
}
