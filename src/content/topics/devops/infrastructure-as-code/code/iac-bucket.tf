# State lives remotely, encrypted and locked, never in git: it holds resource ids and secrets.
terraform {
  required_version = ">= 1.10" # S3 native locking (use_lockfile)
  backend "s3" {
    bucket       = "acme-terraform-state"
    key          = "orders/prod.tfstate"
    region       = "eu-west-1"
    encrypt      = true
    use_lockfile = true # one plan or apply at a time
  }
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" } # pinned, like any dependency
  }
}

variable "environment" {
  type = string
}

# Declare what should exist; Terraform works out the create, update or delete.
resource "aws_s3_bucket" "invoices" {
  bucket = "acme-invoices-${var.environment}"
  tags   = { team = "orders", managed_by = "terraform" }
}

resource "aws_s3_bucket_versioning" "invoices" {
  bucket = aws_s3_bucket.invoices.id # a reference: Terraform orders the work from these
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_public_access_block" "invoices" {
  bucket                  = aws_s3_bucket.invoices.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
