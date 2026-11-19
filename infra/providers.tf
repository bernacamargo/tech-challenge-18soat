provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "oficina"
      ManagedBy   = "terraform"
      Environment = var.environment
    }
  }
}
