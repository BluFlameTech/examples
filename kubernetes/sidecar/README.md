# Sidecar Containers

_This example is associated with the [Sidecar Containers](https://www.bluflametech.com/blog/sidecar-containers/) post._

This is a working implementation of a Kubernetes deployment that contains both a NGINX and an Express container
in a single Pod. The NGINX container is the reverse proxy for the Express container and it is exposed via a Loadbalancer Service.

## Prerequisites

* Docker v20.10.8
* Node v17
* Helm v3.6.3
* Kubernetes v1.21.5

## Directory Structure

* ```express``` - the Express container configuration
* ```nginx``` - the NGINX container configuration
* ```helm``` - the Helm Chart configuration
