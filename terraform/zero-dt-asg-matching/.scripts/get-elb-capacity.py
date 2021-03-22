#!/usr/local/bin/python3

"""
This Python3 script queries AWS for the load balancer based on the name specified in the region specified and
then returns the number of instances currently associated with that load balancer. If the load balancer cannot be
found then the default value is returned.

Example usage in Terraform:

data "external" "elb" {
    program = ["python3", "${path.module}/.scripts/get-elb-capacity.py"]

    query = {
        region = var.region
        name = var.lb_name
        default = 4
    }
}

...

output "elb_capacity" {
    value = data.external.elb.result.capacity
}

Notes: In order for this to work, the shebang at the top of this file should reflect the Python3 installation on the
system being used. Additionally, boto3 must be installed (i.e. pip3 install boto3) and AWS must be configured.
"""

import sys
import json
import boto3
import botocore.exceptions
from botocore.config import Config

REGION = 'region'
LB_NAME = 'name'
DEFAULT = 'default'


def terraform(func):
    def wrapper():
        tf_input = ''
        for line in sys.stdin:
            tf_input += line
        print(json.dumps(func(json.loads(tf_input))))
    return wrapper


@terraform
def get_elb_capacity(query):
    """
    using the terraform_external_data decorator, get_elb_capacity accepts a dict of input values from Terraform,
    validates them and returns the specified load balancer's capacity, if available.
    otherwise, the default value supplied is returned.
    :param query: a dict of input values from Terraform
    :return: a dict containing the load balancer's current capacity
    """

    # pre-condition validation
    if len(query) != 3:
        raise Exception('incorrect number of input values!')
    for key in query:
        if key not in [REGION, LB_NAME, DEFAULT]:
            raise Exception(key + ' is not a valid input value key!')

    # query aws loadbalancer by name
    # Boto elb reference: https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/elb.html#ElasticLoadBalancing.Client.describe_instance_health
    config = Config(
        region_name=query[REGION],
        signature_version='v4',
        retries={
            'max_attempts': 10,
            'mode': 'standard'
        }
    )
    client = boto3.client('elb')

    try:
        load_balancer = client.describe_instance_health(
            LoadBalancerName=query[LB_NAME],
        )
        return {'capacity': str(len(load_balancer['InstanceStates']))}
    except botocore.exceptions.ClientError:
        pass
    return {'capacity': str(query[DEFAULT])}


if __name__ == '__main__':
    get_elb_capacity()
