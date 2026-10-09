locals {
  # Account id keeps the globally unique S3 name from colliding with other accounts.
  bucket_name = "${var.project}-tfstate-${data.aws_caller_identity.current.account_id}"

  tags = {
    project    = var.project
    env        = var.env
    component  = "bootstrap"
    managed-by = "terraform"
  }
}
