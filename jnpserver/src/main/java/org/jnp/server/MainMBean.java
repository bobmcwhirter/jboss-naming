/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jnp.server;

import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.List;

import javax.net.ServerSocketFactory;

/** 
 * The Mbean interface for the jnp provider server.
 * 
 * @author Rickard Oberg
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 */
public interface MainMBean extends NamingBean
{
   // Attributes  ---------------------------------------------------
   
   void setRmiPort(int port);
   int getRmiPort();
   
   void setPort(int port);
   int getPort();

   void setBindAddress(String host) throws UnknownHostException;   
   String getBindAddress();

   void setRmiBindAddress(String host) throws UnknownHostException;   
   String getRmiBindAddress();

   void setBacklog(int backlog);   
   int getBacklog();
   
   /**
    * Returns a URL suitable for use as a java.naming.provider.url value in
    * a set of naming environment properties; i.e. one that can be used to 
    * connect to the lookup socket.
    * <p>
    * If there are {@link #getBootstrapURLs() multiple bootstrap URLs}, returns
    * the first one in the list. TODO: that is is pretty arbitrary
    * </p>
    * 
    * @return the URL, or <code>null</code> if no bound lookup socket exists
    */
   String getBootstrapURL();
   
   /**
    * Returns a list of URLs suitable for use as a java.naming.provider.url 
    * value in a set of naming environment properties; i.e. ones that can be used to 
    * connect to the lookup socket. There will be one URL per configured
    * {@link #getBindAddresses() bind address}.
    * 
    * @return the URLs, or <code>null</code> if no bound lookup socket exists
    */
   List<String> getBootstrapURLs();

   public NamingBean getNamingInfo();
   /**
    * Set the NamingBean/Naming implementation
    * @param info
    */
   public void setNamingInfo(NamingBean info);

   /**
    * Get the call by value flag for jndi lookups.
    * @return true if all lookups are unmarshalled using the caller's TCL, false if in VM lookups return the value by reference.    */
   boolean getCallByValue() ;

   /**
    * Set the call by value flag for jndi lookups.
    * @param flag - true if all lookups are unmarshalled using the caller's TCL, false if in VM lookups return the value by reference.    */
   void setCallByValue(boolean flag) ;

   /** Whether the MainMBean's Naming server will be installed as the NamingContext.setLocal global value */
   void setInstallGlobalService(boolean flag);
   boolean getInstallGlobalService();

   /** Get the UseGlobalService which defines whether the MainMBean's
    * Naming server will initialized from the existing NamingContext.setLocal
    * global value.
    * 
    * @return true if this should try to use VM global naming service, false otherwise 
    */ 
   public boolean getUseGlobalService();
   /** Set the UseGlobalService which defines whether the MainMBean's
    * Naming server will initialized from the existing NamingContext.setLocal global
    * value. This allows one to export multiple servers via different transports
    * and still share the same underlying naming service.
    * 
    * @return true if this should try to use VM global naming service, false otherwise 
    */ 
   public void setUseGlobalService(boolean flag);

   /** The RMIClientSocketFactory implementation class */
   void setClientSocketFactory(String factoryClassName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException;
   String getClientSocketFactory();
   /** The RMIClientSocketFactory bean */
   public RMIClientSocketFactory getClientSocketFactoryBean();
   public void setClientSocketFactoryBean(RMIClientSocketFactory factory);

   /** The RMIServerSocketFactory implementation class */
   void setServerSocketFactory(String factoryClassName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException;
   String getServerSocketFactory();
   /** The RMIServerSocketFactory bean */
   public RMIServerSocketFactory getServerSocketFactoryBean();
   public void setServerSocketFactoryBean(RMIServerSocketFactory factory);

   /** The JNPServerSocketFactory implementation class */
   ServerSocketFactory getJNPServerSocketFactoryBean();
   void setJNPServerSocketFactoryBean(ServerSocketFactory factory);
   public String getJNPServerSocketFactory();
   void setJNPServerSocketFactory(String factoryClassName) 
      throws ClassNotFoundException, InstantiationException, IllegalAccessException;

   /**
    * Get the externally define Naming proxy instance
    * @return
    * @throws Exception
    */
   public Object getNamingProxy()
      throws Exception;
   public void setNamingProxy(Object proxy)
      throws IOException;

   /**
    * Get any exception seen during the lookup listening port creation
    * @return
    */
   public Exception getLookupListenerException();

   // Operations ----------------------------------------------------
   
   public void start() throws Exception;
   
   public void stop() throws Exception;
   
}
