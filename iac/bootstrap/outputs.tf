output "state_bucket_name" {
  description = "Bucket to use in the backend \"s3\" block of the other components."
  value       = aws_s3_bucket.tfstate.bucket
}

output "state_bucket_arn" {
  description = "ARN of the state bucket."
  value       = aws_s3_bucket.tfstate.arn
}
