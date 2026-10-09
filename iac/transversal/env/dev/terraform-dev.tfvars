region              = "us-east-1"
project             = "nequi-franquicias"
env                 = "dev"
vpc_cidr            = "10.0.0.0/16"
availability_zones  = ["us-east-1a", "us-east-1b"]
public_subnet_cidrs = ["10.0.1.0/24", "10.0.2.0/24"]
app_port            = 8080
health_check_path   = "/actuator/health"
