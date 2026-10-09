output "api_id" {
  description = "API where the transversal component attaches the route to the ALB."
  value       = aws_apigatewayv2_api.http.id
}

output "api_endpoint" {
  description = "Fixed public URL of the API."
  value       = aws_apigatewayv2_api.http.api_endpoint
}
