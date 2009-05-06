/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.naming.microcontainer;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerMode;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.logging.Logger;
import org.jboss.util.collection.WeakValueHashMap;
import org.jboss.util.naming.Util;

/**
 * MicrocontainerObjectFactory.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class MicrocontainerObjectFactory implements ObjectFactory
{
   /** The log */
   private static final Logger log = Logger.getLogger(MicrocontainerObjectFactory.class);
   
   /** The bindings to controller contexts */
   private static Map<String, ControllerContext> bindings = Collections.synchronizedMap(new WeakValueHashMap<String, ControllerContext>());

   /**
    * Bind the object
    * 
    * @param binding the binding
    * @param className the class name (can be null)
    * @param ctx the jndi context (can be null)
    * @param controllerContext the controller context
    * @throws NamingException for any error
    * @throws IllegalArgumentException for a null parameter
    */
   public synchronized static void bind(String binding, String className, Context ctx, ControllerContext controllerContext) throws NamingException
   {
      if (binding == null)
         throw new IllegalArgumentException("Null binding");
      if (controllerContext == null)
         throw new IllegalArgumentException("Null controller context");
      
      if (bindings.containsKey(binding))
         throw new NameAlreadyBoundException("The name is already bound: " + binding);
         
      if (className == null)
      {
         Object target = controllerContext.getTarget();
         if (target != null)
            className = target.getClass().getName();
      }
      
      if (ctx == null)
         ctx = new InitialContext();

      // Setup the reference
      String factory = MicrocontainerObjectFactory.class.getName();
      StringRefAddr addr = new StringRefAddr("nns", binding);
      Reference ref = new Reference(className, addr, factory, null);
      bindings.put(binding, controllerContext);

      try
      {
         // This will perform the permission check
         Util.bind(ctx, binding, ref);
         log.debug("Bound '" + controllerContext.getName() + "' to jndi name: " + binding);
      }
      catch (NamingException e)
      {
         bindings.remove(binding);
      }
   }

   /**
    * Unbind the object
    * 
    * @param binding the binding
    * @param ctx the jndi context (can be null)
    * @throws NamingException for any error
    * @throws IllegalArgumentException for a null parameter
    */
   public synchronized static void unbind(String binding, Context ctx) throws NamingException
   {
      if (binding == null)
         throw new IllegalArgumentException("Null binding");
      
      if (bindings.containsKey(binding) == false)
         throw new NameNotFoundException("The name is not bound: " + binding);
      
      if (ctx == null)
         ctx = new InitialContext();

      // This will perform the permission check
      Util.unbind(ctx, binding);

      ControllerContext controllerContext = bindings.remove(binding);
      if (controllerContext != null)
         log.debug("Unound '" + controllerContext.getName() + "' from jndi name: " + binding);
   }
   
   public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception
   {
      // Determine the binding to try
      if (obj == null || obj instanceof Reference == false)
         return null;
      Reference ref = (Reference) obj;
      RefAddr addr = ref.get("nns");
      if (addr == null || addr instanceof StringRefAddr == false)
         return null;
      String binding = (String) addr.getContent();

      // No such binding, let others try to find it
      ControllerContext controllerContext = bindings.get(binding);
      if (controllerContext == null)
         return null;
      
      // Check the context is usable and return the object
      return checkInstalled(controllerContext);
   }

   /**
    * Make sure a context is installed
    * 
    * @param controllerContext the controller context
    * @return the installed object
    * @throws Exception for any error
    */
   private Object checkInstalled(ControllerContext controllerContext) throws Exception
   {
      try
      {
         // Not installed?
         if (ControllerState.INSTALLED.equals(controllerContext.getState()) == false)
         {
            // On demand, lets try to install it
            if (ControllerMode.ON_DEMAND.equals(controllerContext.getMode()))
            {
               Controller controller = controllerContext.getController();
               controller.change(controllerContext, ControllerState.INSTALLED);
            }
         }
      }
      catch (Throwable t)
      {
         if (t instanceof Error)
            throw (Error) t;
         if (t instanceof Exception)
            throw (Exception) t;
         throw new RuntimeException("Error", t);
      }

      // Real check if we are installed
      if (ControllerState.INSTALLED.equals(controllerContext.getState()) == false)
         throw new NamingException("Cannot install controller context: " + controllerContext.getName());
      
      return controllerContext.getTarget();
   }
}
