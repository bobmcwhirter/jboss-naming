/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jnp.test.support;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

import org.jboss.logging.Logger;

/**
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class ServerSocketFactoryBean implements RMIServerSocketFactory
{
   private static final Logger log = Logger.getLogger(ServerSocketFactoryBean.class);
   private InetAddress rmiBindAddress;
   private int backlog = 0;
   
   
   public InetAddress getRmiBindAddress()
   {
      return rmiBindAddress;
   }
   public void setRmiBindAddress(InetAddress rmiBindAddress)
   {
      this.rmiBindAddress = rmiBindAddress;
      // Set the java.rmi.server.hostname to the bind address if not set
      if(System.getProperty("java.rmi.server.hostname") == null)
         System.setProperty("java.rmi.server.hostname", rmiBindAddress.getHostName());
   }

   public int getBacklog()
   {
      return backlog;
   }
   public void setBacklog(int backlog)
   {
      this.backlog = backlog;
   }

   public ServerSocket createServerSocket(int port) throws IOException
   {
      log.info("ServerSocketFactoryBean, createServerSocket port: "+port+", bindAddr: "+rmiBindAddress);
      ServerSocket ss = new ServerSocket(port, backlog, rmiBindAddress);
      return ss;
   }

}
