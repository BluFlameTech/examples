variable "region" {
  type = string
  description = "the region in which to provision"
  default = "us-east-1"
}

variable "app_name" {
  type = string
  description = "the name of the app"
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

variable "userdata" {
  type = string
  description = "the userdata supplied by the calling module"
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
  description = "the desired number of instances in the asg (defaults to min_size)"
  default = ""
}

variable "vpc_id" {
  type = string
  description = "the vpc id"
}

variable "lb" {
  type = object({
    security_group_id = string,
    name = string,
    id = string,
    arn = string
  })
  description = "load balancer info"
}

variable "keypair_name" {
  type = string
  description = "the keypair that will be used for ssh access into the ec2 instances, if supplied"
  default = ""
}
