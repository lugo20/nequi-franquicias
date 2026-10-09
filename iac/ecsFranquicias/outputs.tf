output "api_url" {
  description = "Base URL of the API."
  value       = "http://${local.transversal.alb_dns_name}/api/v1"
}

output "service_name" {
  description = "ECS service name."
  value       = aws_ecs_service.app.name
}

output "log_group" {
  description = "CloudWatch log group of the application."
  value       = aws_cloudwatch_log_group.app.name
}
