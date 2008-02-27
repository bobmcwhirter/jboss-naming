/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jnp.server;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.jboss.logging.Logger;
import org.jboss.naming.ENCFactory;
import org.jnp.interfaces.Naming;
import org.jnp.interfaces.NamingContext;

/**
 * A naming pojo that wraps the Naming server implementation. This is
 * a refactoring of the legacy org.jnp.server.Main into a 
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class NamingBeanImpl
   implements NamingBean
{
   private static Logger log = Logger.getLogger(NamingBeanImpl.class);
   // Attributes ----------------------------------------------------
   /** The Naming interface server implementation */
   protected Naming theServer;
   /** A flag indicating if theServer will be set as the NamingContext.setLocal value */
   protected boolean InstallGlobalService = true;
   /** A flag indicating if theServer will try to use the NamingContext.setLocal value */
   protected boolean UseGlobalService = true;

   // Static --------------------------------------------------------
   public static void main(String[] args)
      throws Exception
   {
      new Main().start();
   }
 
   // Constructors --------------------------------------------------
   public NamingBeanImpl()
   {
   }

   // Public --------------------------------------------------------
   public Naming getNamingInstance()
   {
      return theServer;
   }

   public boolean getInstallGlobalService()
   {
      return InstallGlobalService;
   }
   public void setInstallGlobalService(boolean flag)
   {
      this.InstallGlobalService = flag;
   }
   public boolean getUseGlobalService()
   {
      return UseGlobalService;
   }
   public void setUseGlobalService(boolean flag)
   {
      this.UseGlobalService = flag;
   }

   public void start()
      throws Exception
   {
      // Create the local naming service instance if it does not exist
      if( theServer == null )
      {
         // See if we should try to reuse the current local server
         if( UseGlobalService == true )
            theServer = NamingContext.localServer;
         // If not, or there is no server create one
         if( theServer == null )
            theServer = new NamingServer();
         else
         {
            // We need to wrap the server to allow exporting it
            NamingServerWrapper wrapper = new NamingServerWrapper(theServer);
            theServer = wrapper;
         }
         log.debug("Using NamingServer: "+theServer);
         if( InstallGlobalService == true )
         {
            // Set local server reference
            NamingContext.setLocal(theServer);
            log.debug("Installed global NamingServer: "+theServer);
         }
      }

      /* Create a default InitialContext and dump out its env to show what properties
      were used in its creation. If we find a Context.PROVIDER_URL property
      issue a warning as this means JNDI lookups are going through RMI.
      */
      InitialContext iniCtx = new InitialContext();
      Hashtable env = iniCtx.getEnvironment();
      log.debug("InitialContext Environment: ");
      Object providerURL = null;
      for (Enumeration keys = env.keys(); keys.hasMoreElements(); )
      {
         Object key = keys.nextElement();
         Object value = env.get(key);
         String type = value == null ? "" : value.getClass().getName();
         log.debug("key="+key+", value("+type+")="+value);
         if( key.equals(Context.PROVIDER_URL) )
            providerURL = value;
      }
      // Warn if there was a Context.PROVIDER_URL
      if( providerURL != null )
         log.warn("Context.PROVIDER_URL in server jndi.properties, url="+providerURL);
   
      /* Bind an ObjectFactory to "java:comp" so that "java:comp/env" lookups
         produce a unique context for each thread contexxt ClassLoader that
         performs the lookup.
      */
      ClassLoader topLoader = Thread.currentThread().getContextClassLoader();
      ENCFactory.setTopClassLoader(topLoader);
      RefAddr refAddr = new StringRefAddr("nns", "ENC");
      Reference envRef = new Reference("javax.namingMain.Context", refAddr, ENCFactory.class.getName(), null);
      Context ctx = (Context)iniCtx.lookup("java:");
      ctx.rebind("comp", envRef);
      ctx.close();
      iniCtx.close();

   }

   public void stop()
   {
   }

}
