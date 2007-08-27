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
package org.jnp.interfaces.java;

import java.util.Hashtable;
import javax.naming.*;
import javax.naming.spi.*;

import org.jnp.interfaces.NamingContext;
import org.jnp.interfaces.Naming;

/**
 *   Implementation of "java:" namespace factory. The context is associated
 *   with the thread, so the root context must be set before this is used in a thread
 *      
 *   @see <related>
 *   @author $Author$
 *   @version $Revision$
 */
public class javaURLContextFactory
   implements ObjectFactory
{
   // Constants -----------------------------------------------------
    
   // Attributes ----------------------------------------------------
    
   // Static --------------------------------------------------------
   private static ThreadLocal server = new ThreadLocal();
   
   public static void setRoot(Naming srv)
   {
      // TODO: Add security check here
      server.set(srv);
   }
   
   public static Naming getRoot()
   {
      // TODO: Add security check here
      return (Naming)server.get();
   }
   
   // Constructors --------------------------------------------------
   
   // Public --------------------------------------------------------

   // ObjectFactory implementation ----------------------------------
   public Object getObjectInstance(Object obj,
                                Name name,
                                Context nameCtx,
                                Hashtable environment)
                         throws Exception
   {
      if (obj == null)
         return new NamingContext(environment, name, (Naming)server.get());
      else if (obj instanceof String)
      {
         String url = (String)obj;
         Context ctx = new NamingContext(environment, name, (Naming)server.get());
         
         Name n = ctx.getNameParser(name).parse(url.substring(url.indexOf(":")+1));
         if (n.size() >= 3)
         {
            // Provider URL?
            if (n.get(0).toString().equals("") &&
                n.get(1).toString().equals(""))
            {
               ctx.addToEnvironment(Context.PROVIDER_URL, n.get(2));
            }
         }
         return ctx;
      } else
      {
         return null;
      }
   }
    
   // Y overrides ---------------------------------------------------

   // Package protected ---------------------------------------------
    
   // Protected -----------------------------------------------------
    
   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}