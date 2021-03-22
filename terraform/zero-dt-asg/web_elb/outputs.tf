output "security_group_id" {
  value = aws_security_group.lb.id
}

output "name" {
  value = aws_elb.this.name
}

output "id" {
  value = aws_elb.this.id
}

output "arn" {
  value = aws_elb.this.arn
}

output "dns" {
  value = aws_route53_record.elb.name
}
