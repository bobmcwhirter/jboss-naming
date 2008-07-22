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
package org.jboss.naming;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

import org.jnp.interfaces.NamingContext;
import org.jnp.server.NamingServer;

/**
 *   Implementation of "java:comp" namespace factory. The context is associated
 *   with the thread class loader.
 *     
 *   @author <a href="mailto:rickard.oberg@telkel.com">Rickard Oberg</a>
 *   @author <a href="mailto:scott.stark@jboss.org">Scott Stark</a>
 *   @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 *   @version $Revision$
 */
public class ENCFactory
   implements ObjectFactory
{
   // Constants -----------------------------------------------------
    
   // Attributes ----------------------------------------------------
   private static WeakHashMap classloaderKeyedEncs = new WeakHashMap();
   private static ClassLoader topLoader;
   private static ThreadLocalStack<Object> encIdStack = new ThreadLocalStack<Object>();
   private static ConcurrentHashMap<Object, Context> encById = new ConcurrentHashMap<Object, Context>();

   public static List<Object> getIdStack()
   {
      return encIdStack.getList();
   }

   public static ConcurrentHashMap<Object, Context> getEncById()
   {
      return encById;
   }

   public static void pushContextId(Object id)
   {
      encIdStack.push(id);
   }

   public static Object popContextId()
   {
      return encIdStack.pop();
   }

   public static Object getCurrentId()
   {
      return encIdStack.get();
   }

   // Static --------------------------------------------------------
   
   public static void setTopClassLoader(ClassLoader topLoader)
   {
      ENCFactory.topLoader = topLoader;
   }
   public static ClassLoader getTopClassLoader()
   {
      return ENCFactory.topLoader;
   }

   // Public --------------------------------------------------------

   // ObjectFactory implementation ----------------------------------
   public Object getObjectInstance(Object obj, Name name, Context nameCtx,
      Hashtable environment)
      throws Exception
   {
      Object currentId = encIdStack.get();
      if (currentId != null)
      {
         Context compCtx = encById.get(currentId);
         if (compCtx == null)
         {
            compCtx = createContext(environment);
            encById.put(currentId, compCtx);
         }
         return compCtx;
      }
      // Get naming for this component
      ClassLoader ctxClassLoader = GetTCLAction.getContextClassLoader();
      synchronized (classloaderKeyedEncs)
      {
         Context compCtx = (Context) classloaderKeyedEncs.get(ctxClassLoader);

         /* If this is the first time we see ctxClassLoader first check to see
          if a parent ClassLoader has created an ENC namespace.
         */
         if (compCtx == null)
         {
            ClassLoader loader = ctxClassLoader;
            GetParentAction action = new GetParentAction(ctxClassLoader);
            while( loader != null && loader != topLoader && compCtx == null )
            {
               compCtx = (Context) classloaderKeyedEncs.get(loader);
               loader = action.getParent();
            }
            // If we did not find an ENC create it
            if( compCtx == null )
            {
               compCtx = createContext(environment);
               classloaderKeyedEncs.put(ctxClassLoader, compCtx);
            }
         }
         return compCtx;
      }
   }

   /**
    * Util method for possible override.
    *
    * @return new naming server instance
    * @throws NamingException for any error
    */
   protected NamingServer createServer() throws NamingException
   {
      return new NamingServer();
   }

   protected Context createContext(Hashtable environment)
           throws NamingException
   {
      NamingServer srv = createServer();
      return new NamingContext(environment, null, srv);
   }

   private static class GetTCLAction implements PrivilegedAction
   {
      static PrivilegedAction ACTION = new GetTCLAction();
      public Object run()
      {
         return Thread.currentThread().getContextClassLoader();
      }
      static ClassLoader getContextClassLoader()
      {
         return (ClassLoader) AccessController.doPrivileged(ACTION);
      }
   }

   private static class GetParentAction implements PrivilegedAction
   {
      ClassLoader loader;
      GetParentAction(ClassLoader loader)
      {
         this.loader = loader;
      }
      public Object run()
      {
         ClassLoader parent = null;
         if( loader != null )
         {
            parent = loader.getParent();
            loader = parent;
         }
         return parent;
      }
      ClassLoader getParent()
      {
         return (ClassLoader) AccessController.doPrivileged(this);
      }
   }
}
