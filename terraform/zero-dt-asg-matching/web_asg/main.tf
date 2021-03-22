
data "aws_subnet_ids" "private" {
  vpc_id = var.vpc_id
  tags = {
    tier = "private"
  }
}

resource "aws_iam_role" "this" {
  name = "${var.app_name}-role"
  assume_role_policy = <<-EOT
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Action": "sts:AssumeRole",
        "Principal": {
          "Service": "ec2.amazonaws.com"
        },
        "Effect": "Allow",
        "Sid": ""
      }
    ]
  }
  EOT
}

resource "aws_iam_instance_profile" "this" {
  name = "${var.app_name}-profile"
  role = aws_iam_role.this.name
}

resource "aws_security_group" "instance" {
  name = "${var.app_name}-asg-sg"
  description = "Security group for ${var.app_name}; allows web traffic from the elb"
  vpc_id = var.vpc_id

  ingress {
    from_port = 0
    to_port = 0
    protocol = "ICMP"
    description = "ping access"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 80
    to_port = 80
    protocol = "TCP"
    description = "Web only from ELB"
    security_groups = [var.lb.security_group_id]
  }

  dynamic "ingress" {
    for_each = length(var.keypair_name) > 0 ? ["ssh"] : []
    content {
      from_port = 22
      to_port = 22
      protocol = "TCP"
      description = "SSH access"
      cidr_blocks = ["0.0.0.0/0"]
    }
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_launch_configuration" "this" {
  instance_type = var.instance_type
  image_id = var.ami
  root_block_device {
    delete_on_termination = true
    volume_size = var.volume_size
  }
  key_name = length(var.keypair_name) > 0 ? var.keypair_name : null
  security_groups = [aws_security_group.instance.id]
  user_data = base64encode(templatefile("${path.module}/.scripts/userdata.sh", {
    userdata = var.userdata
  }))
  iam_instance_profile = aws_iam_instance_profile.this.name
  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_autoscaling_group" "this" {
  name = "${var.app_name}-${aws_launch_configuration.this.name}"
  desired_capacity = length(var.desired_capacity) > 0 ? var.desired_capacity : var.min_size
  min_size = var.min_size
  max_size = var.max_size
  vpc_zone_identifier = data.aws_subnet_ids.private.ids
  launch_configuration = aws_launch_configuration.this.name
  load_balancers = [var.lb.id]
  min_elb_capacity = length(var.desired_capacity) > 0 ? var.desired_capacity : var.min_size
lifecycle {
    create_before_destroy = true
  }
}
