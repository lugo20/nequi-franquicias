output "api_url" {
  description = "Fixed public base URL of the API (API Gateway)."
  value       = "${local.api_gateway.api_endpoint}/api/v1"
}

output "service_name" {
  description = "ECS service name."
  value       = aws_ecs_service.app.name
}

output "log_group" {
  description = "CloudWatch log group of the application."
  value       = aws_cloudwatch_log_group.app.name
}
