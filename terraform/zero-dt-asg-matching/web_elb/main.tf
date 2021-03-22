
data "aws_subnet_ids" "public" {
  vpc_id = var.vpc_id
  tags = {
    tier = "public"
  }
}

data "aws_route53_zone" "public" {
  name         = var.public_domain
  private_zone = false
}

data "aws_acm_certificate" "this" {
  domain = "*.${var.public_domain}"
  statuses = ["ISSUED"]
}

resource "aws_security_group" "lb" {
  name = "${var.app_name}-lb-sg"
  description = "Security group for load balancer; allows web traffic to instance"
  vpc_id = var.vpc_id

  ingress {
    from_port = 443
    to_port = 443
    protocol = "TCP"
    description = "Secure Web traffic"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_elb" "this" {
  name = "${var.app_name}-lb"
  subnets = data.aws_subnet_ids.public.ids
  security_groups = [aws_security_group.lb.id]
  listener {
    instance_port = 80
    instance_protocol = "http"
    lb_port = 443
    lb_protocol = "https"
    ssl_certificate_id = data.aws_acm_certificate.this.arn
  }

  health_check {
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 3
    target              = "TCP:80"
    interval            = 30
  }
}

//add dns to the load balancer
resource "aws_route53_record" "elb" {
  zone_id = data.aws_route53_zone.public.zone_id
  name    = "${var.app_name}.${var.public_domain}"
  type    = "A"

  alias {
    evaluate_target_health = false
    name = aws_elb.this.dns_name
    zone_id = aws_elb.this.zone_id
  }
}