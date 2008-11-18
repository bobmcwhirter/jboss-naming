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
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

import org.jboss.logging.Logger;

/**
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class ClientSocketFactoryBean
   implements RMIClientSocketFactory, Serializable
{
   private static final Logger log = Logger.getLogger(ClientSocketFactoryBean.class);
   private static final long serialVersionUID = 1;

   public Socket createSocket(String host, int port) throws IOException
   {
      log.info("ClientSocketFactoryBean, createSocket host:"+host+", port:"+port);
      Socket s = new Socket(host, port);
      s.setSoTimeout(5000);
      return s;
   }
}
