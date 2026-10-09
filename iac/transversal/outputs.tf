output "vpc_id" {
  description = "VPC where the ECS service runs."
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "Subnets where the ALB and the ECS tasks are placed."
  value       = module.vpc.public_subnets
}
