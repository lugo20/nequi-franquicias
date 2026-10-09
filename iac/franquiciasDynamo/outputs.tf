output "table_name" {
  description = "Value for the DYNAMODB_TABLE_NAME variable of the application."
  value       = aws_dynamodb_table.franchises.name
}

output "table_arn" {
  description = "ARN used to grant the ECS task access to the table."
  value       = aws_dynamodb_table.franchises.arn
}
