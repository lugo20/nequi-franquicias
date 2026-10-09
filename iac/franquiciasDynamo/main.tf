################################################################################
# DynamoDB - Single table for franchises, branches, products and name reservations
################################################################################

resource "aws_dynamodb_table" "franchises" {
  name         = local.table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "franchiseKey"
  range_key    = "entityKey"

  attribute {
    name = "franchiseKey"
    type = "S"
  }

  attribute {
    name = "entityKey"
    type = "S"
  }

  # Encryption at rest is always on, with an AWS owned key at no extra cost.
}
