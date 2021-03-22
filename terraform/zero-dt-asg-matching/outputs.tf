output "s3_bucket" {
  description = "the s3 bucket used to deploy the tar.gz arfiact (contains the Dockerfile and any other resources needed by the Dockerfile)"
  value = aws_s3_bucket.this
}

output "asg" {
  description = "the autoscaling group details"
  value = module.asg
}

output "elb" {
  description = "the classic elb details (used to route web requests to the instances in the autoscaling group)"
  value = module.elb
}