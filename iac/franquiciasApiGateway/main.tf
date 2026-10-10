################################################################################
# API Gateway - Public HTTPS entry point with a fixed URL.
# Never destroyed: its URL survives recreating the network and the service.
# The route and the private integration to the ALB live in the transversal component.
################################################################################

resource "aws_apigatewayv2_api" "http" {
  name          = local.name
  protocol_type = "HTTP"
  description   = "Public entry point of the franchises API"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.http.id
  name        = "$default"
  auto_deploy = true
}
