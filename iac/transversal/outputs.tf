output "vpc_id" {
  description = "VPC where the ECS service runs."
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "Subnets where the ALB and the ECS tasks are placed."
  value       = module.vpc.public_subnets
}

output "alb_dns_name" {
  description = "Public URL of the API."
  value       = module.alb.dns_name
}

output "target_group_arn" {
  description = "Target group where the ECS service registers its tasks."
  value       = module.alb.target_groups["app"].arn
}

output "tasks_security_group_id" {
  description = "Security group assigned to the ECS tasks."
  value       = aws_security_group.tasks.id
}

output "ecs_cluster_name" {
  description = "ECS cluster where the service runs."
  value       = aws_ecs_cluster.this.name
}
