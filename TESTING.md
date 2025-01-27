### Requirements
Running Ion requires Redis, and MongoDB. While Redis is easy and should just work after it is installed and running, MongoDB is a little more complicated.

#### Linux
For MongoDB you need to ensure that MongoDB is started with the options `--replSet rs0 --bind_ip_all`. How to set this will vary from system to system, so please look for the appropriate documentation, if you can't find it then feel free to ask but please ACTUALLY SPEND MORE THAN A FEW MINUTES LOOKING.

#### Windows
Head to https://www.mongodb.com/try/download/community after running the MSI installer look in programfiles/MongoDB/server/5.0/bin/mongod.cfg (open with notepad).

Find where it says bindIP: change it so it says `bindIP: localhost`. You're half way there, good job! Then where it says #replication: remove the # and add `replSetName: "rs0"` to the line below replication:

Now you have to restart MongoDB, right click on the start button and click on computer management. On the left you'll see *"Services and Applications"* and then *"Services"*. In services look for MongoDB and right click it and look for where it says restart. Now start up MongoDBCompass. And now execute the rest of the installation process below.

Once MongoDB is started, you need to open a mongo shell in your terminal or your "command prompt" for the Windows users, and run `rs.initiate()` and then `db.createUser({user:"test",pwd:"test",roles:[{role:"readWrite",db:"test"}]});`. Feel free to change the username, password, and database name however you will need to remember what you changed it to later. 

Next you will need Redis. On Windows this is more painful. You will need to download  https://github.com/dmajkic/redis/downloads. It's an older version of Redis but works with Ion. After installation, extract the file and open it up. Inside look for the folder labelled 64bit. Click on the redis-server.exe and that's it. You're done.
That's redis working. You will have to open it up between computer restarts but besides that you're good to go.

### Building the plugin
Build the plugin using the provided run configuration in IntelliJ or alternatively run the gradle task "`ionBuild`" either in your IDE or from the terminal:

Linux `./gradlew ionBuild`

Windows `gradlew.bat ionBuild`

MacOS X *why are you using a mac?*

The jar file of Ion will be `./build/Ion.jar`

### Setting up the test server.
Grab yourself a Paper 1.18.1 jar file from (https://papermc.io/downloads) and go through the normal setup process for a server.

Then it's time for dependencies... oh boy.

- Dynmap from (https://github.com/webbukkit/dynmap/releases)
- WorldEdit from (https://dev.bukkit.org/projects/worldedit/files) **OR** FastAsyncWorldEdit from (https://intellectualsites.github.io/download/fawe.html)
- Luckperms from (https://luckperms.net/download)
- Citizens from (https://ci.citizensnpcs.co/job/Citizens2/)

### Configuration
If earlier you set custom database login details you will need to configure them in the `config.yml` file created by the plugin after first launch.

### Running the test server
Just start the server lol
