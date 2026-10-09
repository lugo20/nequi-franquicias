# Outputs of the components this service depends on, read from their remote state.

data "terraform_remote_state" "transversal" {
  backend = "s3"
  config = {
    bucket = var.state_bucket
    key    = "transversal/${var.env}/terraform.tfstate"
    region = var.region
  }
}

data "terraform_remote_state" "dynamo" {
  backend = "s3"
  config = {
    bucket = var.state_bucket
    key    = "franquiciasDynamo/${var.env}/terraform.tfstate"
    region = var.region
  }
}

data "terraform_remote_state" "ecr" {
  backend = "s3"
  config = {
    bucket = var.state_bucket
    key    = "franquiciasEcr/${var.env}/terraform.tfstate"
    region = var.region
  }
}
