# Fixed-URL API created by the franquiciasApiGateway component.
data "terraform_remote_state" "api_gateway" {
  backend = "s3"
  config = {
    bucket = var.state_bucket
    key    = "franquiciasApiGateway/${var.env}/terraform.tfstate"
    region = var.region
  }
}
