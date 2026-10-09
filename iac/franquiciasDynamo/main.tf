################################################################################
# DynamoDB - Single table for franchises, branches, products and name reservations
################################################################################

resource "aws_dynamodb_table" "franchises" {
  name         = local.table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "pk"
  range_key    = "sk"

  attribute {
    name = "pk"
    type = "S"
  }

  attribute {
    name = "sk"
    type = "S"
  }

  # Encryption at rest is always on, with an AWS owned key at no extra cost.
}
