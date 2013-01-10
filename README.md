Sleep-On-LAN
============

Wake-on-LAN is widely implemented in consumer hardware. However, more advanced remote power management is generally
restricted to server-specific hardware. This means that for most home users, powering down a remote machine must be
done manually or via custom scripts.

Sleep-on-LAN provides a mechanism for powering down a computer remotely. It attempts to emulate the existing
[Wake-on-LAN](http://en.wikipedia.org/wiki/Wake-on-LAN) standard as closely as possible.

Requirements
------------

Sleep-on-LAN is written in Java and runs on the [Apache Commons Daemon](http://commons.apache.org/daemon/), which
requires the JRE. The build script requires Apache Maven, but manual build is also possible.

Configuration
-------------

Configuration is done via the sleeponlan.properties file in the resources directory. 

	- port: the port on which to listen (default 9)
	- interface: Specify the network interface on which the service should listen (default eth0)
	- shutdown: the shutdown command should be configured to suit your system (default shutdown -h +1)

Build
-----

Build by running the Maven Assembly command: 
	
	`mvn assembly:assembly`

in the main project directory. This will generate a jar file called SleepOnLAN-1.0-SNAPSHOT-jar-with-dependencies.jar
in the target directory.

Alternatively, you may manually create this file with the java -jar command. Ensure that the properties file and Apache
Commons Daemon library are on the classpath and included in the jar file. Note that running from a jar file, while
recommended, is not obligatory.

Deployment
----------

Copy the SleepOnLAN-1.0-SNAPSHOT-jar-with-dependencies.jar jar file to your target machine. Run using the jsvc/procrun
command. It should be run as a user with permission to listen on the specific port (eg root if using port 9). Be aware
that jsvc has various idiosyncrasies; it is recommended that you test with the -debug flag on to discover any jsvc
issues before attempting to use the service.

Running as root without specifying a user in a UNIX-like environment is a potential security risk. You should specify a
user with the appropriate permissions to shut down the computer.

Your service call can simply be one line in rc.local, but it should be simple to create an init.d wrapper if that is
preferred. An example service call:

	`/usr/bin/jsvc -user sleeper -cp SleepOnLAN-1.0-SNAPSHOT-jar-with-dependencies.jar uk.co.propter.sleeponlan.SleepService`

Usage
-----

The sleep service will attempt to shut its host computer down when it receives a Sleep-on-LAN magic packet. This
resembles the Wake-on-LAN magic packet exactly except that instead of starting with six bytes of 0xFF it starts with
six bytes of 0x00.

The script onoff.py is included as an example of Sleep-on-LAN magic packet generation. It can also send a Wake-on-LAN
magic packet for comparison. Pass it the command wake or sleep and the target machine's MAC address, eg:

	`python onoff.py sleep 00:11:22:33:44:55`
