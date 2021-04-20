output "iam_role" {
  value = aws_iam_role.this.name
}

output "security_group" {
  value = aws_security_group.instance
}

output "ami_id" {
  value = aws_launch_configuration.this.image_id
}

output "key_pair" {
  value = aws_launch_configuration.this.key_name
}

output "instance_type" {
  value = aws_launch_configuration.this.instance_type
}

output "name" {
  value = aws_autoscaling_group.this.name
}

output "desired_capacity" {
  value = aws_autoscaling_group.this.desired_capacity
}

output "min_size" {
  value = aws_autoscaling_group.this.min_size
}

output "max_size" {
  value = aws_autoscaling_group.this.max_size
}

output "availability_zones" {
  value = aws_autoscaling_group.this.availability_zones
}
