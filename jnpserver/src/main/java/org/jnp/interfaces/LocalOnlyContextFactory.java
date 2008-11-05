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

import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.jboss.logging.Logger;

/**
 * An InitialContextFactory that uses the either NamingContex.localServer naming
 * server which has to have been set, or injected.
 * 
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 */
public class LocalOnlyContextFactory implements InitialContextFactory
{
   private static final Logger log = Logger.getLogger(LocalOnlyContextFactory.class);

   /** A set of Naming instances with names */
   private static ConcurrentHashMap<String, WeakReference<Naming>> localServers = new ConcurrentHashMap<String, WeakReference<Naming>>();
   private Naming naming;

   public Naming getNaming()
   {
      return naming;
   }
   /**
    * Set the Naming instance to use for the root content.
    * @param naming - the Naming instance to use, null if the global
    * NamingContext.getLocal value should be used.
    */
   public void setNaming(Naming naming)
   {
      this.naming = naming;
   }

   // InitialContextFactory implementation --------------------------
   public Context getInitialContext(Hashtable<?,?> env)
      throws NamingException
   {
      boolean trace = log.isTraceEnabled();
      if(trace)
         log.trace("getInitialContext, env: "+env);
      String name = null;
      Naming localServer = null;
      if(env != null)
      {
         // First try the naming property instance
         localServer = (Naming) env.get(NamingContext.JNP_NAMING_INSTANCE);
         if(trace && localServer != null)
            log.trace("Set naming from "+NamingContext.JNP_NAMING_INSTANCE);
         name = (String) env.get(NamingContext.JNP_NAMING_INSTANCE_NAME);
      }
      // Next try the injected naming instance
      if (localServer == null)
      {
         localServer = naming;
         if(trace && localServer != null)
            log.trace("Set naming from injected value");
      }
      // Next try to locate the instance by name
      if (localServer == null && name != null)
      {
         WeakReference<Naming> lswr = localServers.get(name);
         if(lswr != null)
            localServer = lswr.get();
         if(trace && localServer != null)
            log.trace("Set naming from localServers#"+name);
      }
      // Lastly try the JVM global NamingContext.local value
      if (localServer == null)
      {
         localServer = NamingContext.getLocal();
         if(trace && localServer != null)
            log.trace("Set naming from NamingContext.getLocal");
      }

      if (localServer == null)
         throw new NamingException("Failed to determine local server from: "+env);

      // If this is a named instance add it to the localServers set
      if (name != null && localServers.containsKey(name) == false)
      {
         localServers.put(name, new WeakReference<Naming>(localServer));
         if(trace)
            log.trace("Set localServers:"+name);
      }

      // Pass in an empty env if its null
      if(env == null)
         env = new Hashtable();
      return new NamingContext(env, null, localServer);
   }
}
