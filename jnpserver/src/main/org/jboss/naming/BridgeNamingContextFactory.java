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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.jnp.interfaces.NamingContextFactory;

/** A naming provider InitialContextFactory implementation that combines
 two Naming services to allow for delegation of lookups from one to the
 other. The default naming service is specified via the standard
 Context.PROVIDER_URL property while the secondary service is specified
 using an org.jboss.naming.provider.url2 property. An example of where
 this would be used is to bridge between the local JNDI service and the
 HAJNDI service. Lookups into the local JNDI service that fail would then
 try the HAJNDI service.

 @see javax.naming.spi.InitialContextFactory
 
 @author Scott.Stark@jboss.org
 @version $Revision$
 */
public class BridgeNamingContextFactory extends NamingContextFactory
{
   // InitialContextFactory implementation --------------------------
   public Context getInitialContext(Hashtable env)
      throws NamingException
   {
      Context primaryCtx = super.getInitialContext(env);
      Context bridgeCtx = primaryCtx;
      Object providerURL2 = env.get("org.jboss.naming.provider.url2");
      if( providerURL2 != null )
      {
         // A second provider url was given, create a secondary naming context
         Hashtable env2 = (Hashtable) env.clone();
         env2.put(Context.PROVIDER_URL, providerURL2);
         Context secondaryCtx = super.getInitialContext(env2);
         InvocationHandler h = new BridgeContext(primaryCtx, secondaryCtx);
         Class[] interfaces = {Context.class};
         ClassLoader loader = Thread.currentThread().getContextClassLoader();
         bridgeCtx = (Context) Proxy.newProxyInstance(loader, interfaces, h);
      }
      return bridgeCtx;
   }

   /** This class is the Context interface handler and performs the
       failed lookup delegation from the primary to secondary naming
       Context.
   */
   static class BridgeContext implements InvocationHandler
   {
      private Context primaryCtx;
      private Context secondaryCtx;

      BridgeContext(Context primaryCtx, Context secondaryCtx)
      {
         this.primaryCtx = primaryCtx;
         this.secondaryCtx = secondaryCtx;
      }

      public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable
      {
         Object value = null;
         // First try the primary context
         try
         {
            value = method.invoke(primaryCtx, args);
         }
         catch(InvocationTargetException e)
         {
            Throwable t = e.getTargetException();
            // Try the secondary if this is a failed lookup
            if( t instanceof NameNotFoundException && method.getName().equals("lookup") )
            {
               try
               {
                  value = method.invoke(secondaryCtx, args);
               }
               catch (InvocationTargetException e1)
               {
                  throw e1.getTargetException();
               }
            }
            else
            {
               throw t;
            }
         }
         return value;
      }
   }
}
