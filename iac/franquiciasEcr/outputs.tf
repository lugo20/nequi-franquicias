output "repository_url" {
  description = "Registry URL used to tag and push the application image."
  value       = aws_ecr_repository.app.repository_url
}

output "repository_name" {
  description = "Repository name."
  value       = aws_ecr_repository.app.name
}
