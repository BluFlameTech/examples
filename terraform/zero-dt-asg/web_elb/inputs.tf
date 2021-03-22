variable "region" {
  type = string
  description = "the region in which to provision"
  default = "us-east-1"
}

variable "app_name" {
  type = string
  description = "the name of the app"
}

variable "public_domain" {
  type = string
  description = "the public domain name (i.e. 'example.com')"
}

variable "vpc_id" {
  type = string
  description = "the vpc id"
}
