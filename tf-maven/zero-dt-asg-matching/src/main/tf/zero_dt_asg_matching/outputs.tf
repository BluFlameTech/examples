output "asg" {
  description = "the autoscaling group details"
  value = module.asg
}

output "elb" {
  description = "the classic elb details (used to route web requests to the instances in the autoscaling group)"
  value = module.elb
}