/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
package org.jnp.interfaces;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import javax.net.SocketFactory;

import org.jboss.logging.Logger;

/** A concrete implementation of the SocketFactory that supports a configurable
 timeout for the initial socket connection as well as the SO_TIMEOUT used to
 determine how long a read will block waiting for data.

@author Scott.Stark@jboss.org
@version $Revision$
 */
public class TimedSocketFactory extends SocketFactory
{
   private static Logger log = Logger.getLogger(TimedSocketFactory.class);
   public static final String JNP_TIMEOUT = "jnp.timeout";
   public static final String JNP_SO_TIMEOUT = "jnp.sotimeout";

   /** The connection timeout in milliseconds */
   protected int timeout = 0;
   /** The SO_TIMEOUT in milliseconds */
   protected int soTimeout = 0;

   /** Creates a new instance of TimedSocketFactory */
   public TimedSocketFactory()
   {
   }
   public TimedSocketFactory(Hashtable<String, ?> env)
   {
      String value = (String) env.get(JNP_TIMEOUT);
      if( value != null )
         timeout = Integer.parseInt(value);
      value = (String) env.get(JNP_SO_TIMEOUT);
      if( value != null )
         soTimeout = Integer.parseInt(value);
   }

   public Socket createSocket(String host, int port) throws IOException, UnknownHostException
   {
      InetAddress hostAddr = InetAddress.getByName(host);
      return this.createSocket(hostAddr, port, null, 0);
   }
   public Socket createSocket(InetAddress hostAddr, int port) throws IOException
   {
      return this.createSocket(hostAddr, port, null, 0);
   }

   public Socket createSocket(String host, int port, InetAddress localAddr, int localPort)
      throws IOException, UnknownHostException
   {
      InetAddress hostAddr = InetAddress.getByName(host);
      return this.createSocket(hostAddr, port, localAddr, localPort);
   }
   public Socket createSocket(InetAddress hostAddr, int port, InetAddress localAddr, int localPort)
      throws IOException
   {
      log.debug("createSocket, hostAddr: "+hostAddr+", port: "+port+", localAddr: "+localAddr+", localPort: "+localPort+", timeout: "+timeout);
      Socket socket = new Socket();
      InetSocketAddress connectAddr = new InetSocketAddress(hostAddr, port);
      if(localAddr != null)
      {
         // Bind 
         InetSocketAddress bindAddr = new InetSocketAddress(localAddr, localPort);
         socket.bind(bindAddr);
      }
      socket.setSoTimeout(soTimeout);
      socket.connect(connectAddr, timeout);
      return socket;
   }

}
