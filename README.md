# cf-minecraft-app
Run a Minecraft server on CloudFoundry

#### S3 Storage

cf-minecraft-app will store all Minecraft server data (world, player information, etc) on an Amazon S3 or compatible storage space.
It will backup world data once per hour.

#### TODO / Problems

* Need to create a WebSocket server/client to wrap around the Minecraft TCP connection, because CloudFoundry only allows HTTP(S) and WebSocket inbound traffic to an app container (really annoying imho, kinda defeats the purpose of this project if an addon client/tunneling tool is needed just to be able to connect to the Minecraft server)
* Or maybe use SuperTunnel/httptunnel? A similar kind of setup like cf-ssh is doing? (again, defeats the purpose of this project. Why would I want to run a Minecraft server on a PaaS, if I need yet another host somewhere to relay/proxy all traffic?)
* frustrating.. :unamused:

Does anyone have good/better idea on how to solve the connection problem of running a Minecraft server on CloudFoundry?

