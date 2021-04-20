### Install Node ###
curl -sL https://rpm.nodesource.com/setup_15.x | sudo -E bash -
sudo yum install -y nodejs

sudo mkdir -p /opt/express
cd /opt/express

### Configure express NPM dependency ###
sudo npm install express

### Configure express server ###
sudo tee -a /opt/express/server.js > /dev/null <<EOT
const express = require('express')
const app = express()
const port = 8080

app.get('/', (req, res) =>
  res.send("${message}")
);

app.listen(port, () =>
    console.log("Server is running on port " + port + "...")
);
EOT

### Run express server ###
sudo nohup npm run start &