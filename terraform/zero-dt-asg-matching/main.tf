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

resource "aws_s3_bucket" "this" {
  bucket = local.unique_app_name
  acl = "private"
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.id
  block_public_policy = true
  block_public_acls = true
  ignore_public_acls = true
  restrict_public_buckets = true
}

//policy to be attached to the role associated with the autoscaling group ec2 instances to give access to the s3 bucket
resource "aws_iam_policy" "s3_bucket_access" {
  policy = templatefile("${path.module}/.policies/s3-bucket-access-policy.json", {
    bucket_arn = aws_s3_bucket.this.arn
  })
}

resource "aws_iam_role_policy_attachment" "s3_bucket_access_to_asg" {
  role = module.asg.iam_role
  policy_arn = aws_iam_policy.s3_bucket_access.arn
}

//upload the tar file to the bucket using the name of the tarfile, minus the path reference
resource "aws_s3_bucket_object" "tar" {
  bucket = aws_s3_bucket.this.id
  key = regex("([^/]+$)", var.tar_file)[0]
  source = "${path.module}/${var.tar_file}"
  etag = timestamp()
}

module "asg" {
  depends_on = [aws_s3_bucket_object.tar]
  source = "./web_asg"
  app_name = local.unique_app_name
  vpc_id = var.vpc_id
  lb = module.elb
  max_size = var.max_size
  min_size = var.min_size
  desired_capacity = data.external.elb.result.capacity
  userdata = templatefile("${path.module}/.scripts/userdata.sh", {
    s3_bucket = aws_s3_bucket.this.id,
    tar_file = regex("([^/]+$)", var.tar_file)[0],
    image_name = var.app_name
    config_version = var.config_version
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
