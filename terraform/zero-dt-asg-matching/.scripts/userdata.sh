sudo yum install -y docker
sudo service docker start
sudo systemctl enable docker
sudo usermod -a -G docker ec2-user
ls webapp || mkdir webapp
aws s3 cp s3://${s3_bucket}/${tar_file} .
tar -C ./webapp -xvzf ${tar_file}
rm ${tar_file}
cd webapp
sudo docker build -t ${image_name} .
sudo docker run -d --restart unless-stopped -p 8080:8080 ${image_name}
echo "${config_version}"