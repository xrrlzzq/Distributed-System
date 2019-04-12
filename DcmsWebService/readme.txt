This application is developed in windows 

excute step:

1.first you should open mtl,lvl,ddo server in 3 different PC (otherwise the port would be conflict)before open the server, you should write the corresponding ip address in the config.properties file(both ip address and hostname is acceptable ),the config.properties file is located at bin dictionary

2.second open cmd and use cd command to access bin dictionary and use server  command "java server.CenterServer serverid"(eg: java server.CenterServer mtl) to open server, the server shuold be in the different pc

3.third open cmd and use cd command to access bin dictionary and use client command "java client.ManagerClient managerid"(eg: java client.ManagerClient mtl0001)to open client,the mutiple client can be open in the same pc

   