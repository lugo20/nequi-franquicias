################################################################################
# ECR - Docker images of the application
################################################################################

resource "aws_ecr_repository" "app" {
  name = local.repository_name
  # Each image is pushed with a unique tag (commit hash) and can never be overwritten.
  image_tag_mutability = "IMMUTABLE"
  # Test project: allows a full cleanup with terraform destroy.
  force_delete = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep only the last ${var.images_to_keep} images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = var.images_to_keep
      }
      action = { type = "expire" }
    }]
  })
}
