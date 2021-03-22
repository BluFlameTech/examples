variable "region" {
  type = string
  description = "the region in which to provision"
  default = "us-east-1"
}

variable "vpc_id" {
  type = string
  description = "the id of the vpc in which to target"
}

variable "app_name" {
  type = string
  description = "the name of the app"
}

variable "config_version" {
  type = string
  description = "the config version (note: an update triggers a redeploy)"
}

variable "public_domain" {
  type = string
  description = "the public domain name (i.e. 'example.com')"
}

variable "instance_type" {
  type = string
  description = "the type of instance (e.g. t3.medium)"
  default = "t3.medium"
}

variable "ami" {
  type = string
  description = "the ami used to provision the ec2 instance"
  default = "ami-0be2609ba883822ec"
}

variable "volume_size" {
  type = string
  description = "the root volume size of the asg instances"
  default = "64"
}

variable "min_size" {
  type = string
  description = "the minimum number of instances in the asg"
  default = "2"
}

variable "max_size" {
  type = string
  description = "the maximum number of instances in the asg"
  default = "4"
}

variable "desired_capacity" {
  type = string
  description = "the desired number of instances in the asg (defaults to min_size if not specified)"
  default = ""
}

variable "keypair_name" {
  type = string
  description = "the keypair that will be used for ssh access into the ec2 instances, if supplied"
  default = ""
}

variable "tar_file" {
  type = string
  description = "the location of the gzipped tar file containing a Dockerfile and any other artifacts referenced by the Dockerfile"
}
