terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
      version = ">= 3.28"
    }
    random = {
      source = "hashicorp/random"
      version = ">= 3.0.0"
    }
  }
}