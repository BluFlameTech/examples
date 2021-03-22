#!/bin/bash -xe
exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

sudo yum update -y
sudo yum install -y gcc-c++ make
sudo yum install -y httpd

${userdata}

### Configure Apache Web Server as a reverse proxy ###

sudo tee -a /etc/httpd/conf.modules.d/app_proxy.conf > /dev/null <<EOT
ProxyRequests       Off
ProxyPreserveHost   On

ProxyPass           / http://localhost:8080/ connectiontimeout=240 timeout=1200
ProxyPassReverse    / http://localhost:8080/

<IfModule mpm_worker_module>
    ServerLimit 100
    StartServers 3
    MinSpareThreads 25
    MaxSpareThreads 75
    ThreadLimit 64
    ThreadsPerChild 25
    MaxClients 2500
    MaxRequestsPerChild 0
</IfModule>
EOT

### Configure and start Apache Web Server
sudo systemctl enable httpd
sudo service httpd start
