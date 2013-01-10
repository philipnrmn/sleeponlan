/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package uk.co.propter.sleeponlan;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SleepService implements Daemon {
    Logger logger;
    DatagramSocket socket;
    int listeningPort;
    String networkInterface;
    String shutdownCommand;
    byte[] macAddress;
    boolean listening;

    @Override
    public void init(DaemonContext context) {
        makeLogger();
        loadProperties();
        openSocket();
        discoverMACAddress();
    }

    private void makeLogger() {
        logger = Logger.getLogger(SleepService.class.getName());
        logger.setLevel(Level.ALL);
    }

    private void loadProperties() {
        Properties properties = new Properties();
        try {
            InputStream inputStream = getClass().getResourceAsStream("/sleeponlan.properties");
            properties.load(inputStream);
        } catch (IOException e) {
            logger.warning("Could not load properties file. Proceeding with default values.");
        }
        listeningPort = Integer.parseInt(properties.getProperty("listening.port", "9"));
        networkInterface = properties.getProperty("network.interface", "eth0");
        shutdownCommand = properties.getProperty("shutdown.command", "shutdown -h +1");
    }

    private void openSocket() {
        try {
            socket = new DatagramSocket(listeningPort);
        } catch (SocketException e) {
            logger.severe("Could not bind socket on port " + listeningPort);
            logger.fine(e.getMessage());
            System.exit(1);
        }
    }

    private void discoverMACAddress() {
        try {
            NetworkInterface ni = NetworkInterface.getByName(networkInterface);
            macAddress = ni.getHardwareAddress();
        } catch (SocketException e) {
            logger.severe("Could not discover MAC address.");
            System.exit(1);
        }
    }

    @Override
    public void start() {
        new Thread() {
            @Override
            public void run() {
                listen(this);
            }
        }.start();
    }

    private void listen(Thread thread) {
        listening = true;
        while (listening) {
            DatagramPacket packet = receivePacket();
            if (isSleepPacket(packet)) {
                logger.info("Received sleep packet.");
                listening = false;
                shutdown();
            } else {
                logger.info("Received packet, but not sleep packet.");
            }
        }
    }

    private void shutdown() {
        try {
            Runtime.getRuntime().exec(shutdownCommand);
        } catch (IOException e) {
            logger.severe("Could not execute shutdown command.");
            logger.fine(e.getMessage());
        }
        System.exit(0);
    }

    private DatagramPacket receivePacket() {
        DatagramPacket packet = new DatagramPacket(new byte[256], 256);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            logger.warning("Received bad packet.");
        }
        return packet;
    }

    private boolean isSleepPacket(DatagramPacket packet) {
        byte[] data = packet.getData();
        return startsWithZeroBytes(data) && containsSixteenMacAddresses(data);
    }

    private boolean startsWithZeroBytes(byte[] bytes) {
        for (int i = 0; i < 6; i++) {
            if (bytes[i] != 0x00)
                return false;
        }
        return true;
    }

    private boolean containsSixteenMacAddresses(byte[] bytes) {
        for (int i = 6; i < 102; i++) {
            if (bytes[i] != macAddress[i % 6])
                return false;
        }
        return true;
    }

    @Override
    public void stop() {
        listening = false;
        socket.close();
    }

    @Override
    public void destroy() {}
}
