provider "aws" {
  region = var.region
}

locals {
  unique_app_name = "${var.app_name}-${random_pet.this.id}"
}

data "external" "elb" {
  program = ["python3", "${path.module}/.scripts/get-elb-capacity.py"]

  query = {
    region = var.region
    name = "${local.unique_app_name}-lb"
    default = length(var.desired_capacity) > 0 ? var.desired_capacity : var.min_size
  }
}

resource "random_pet" "this" {}

module "asg" {
  source = "./web_asg"
  app_name = local.unique_app_name
  vpc_id = var.vpc_id
  lb = module.elb
  max_size = var.max_size
  min_size = var.min_size
  desired_capacity = data.external.elb.result.capacity
  userdata = templatefile("${path.module}/.scripts/userdata.sh", {
    message = var.message
  })
  keypair_name = var.keypair_name
}

module "elb" {
  source = "./web_elb"
  app_name = local.unique_app_name
  vpc_id = var.vpc_id
  public_domain = var.public_domain
  region = var.region
}
