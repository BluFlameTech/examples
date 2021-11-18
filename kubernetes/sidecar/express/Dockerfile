FROM node:17

#use bash shell
SHELL ["/bin/bash", "-c"]

#configure express user
RUN adduser --disabled-password --home /home/express express
USER express:express
ENV HOME=/home/express

#copy over necessary files
COPY package.json ${HOME}/package.json
COPY app.js ${HOME}/app.js

#run the app
WORKDIR ${HOME}
RUN npm install
ENTRYPOINT npm start
