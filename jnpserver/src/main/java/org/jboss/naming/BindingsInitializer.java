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
package org.jboss.naming;

import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

/**
 * A bean that can be used to create JNDI bindings
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class BindingsInitializer
{
   private static Logger log = Logger.getLogger(BindingsInitializer.class);
   private Map<String, ?> bindings;
   private InitialContext ctx;

   /**
    * The key/value bindings to add to jndi
    * @return The key/value bindings to add to jndi
    */
   public Map<String, ?> getBindings()
   {
      return bindings;
   }
   public void setBindings(Map<String, ?> bindings)
   {
      this.bindings = bindings;
   }
   /**
    * Get the InitialContext into which the bindings will be made.
    * @return
    */
   public InitialContext getCtx()
   {
      return ctx;
   }
   /**
    * Set the InitialContext into which the bindings will be made.
    * @param ctx
    */
   public void setCtx(InitialContext ctx)
   {
      this.ctx = ctx;
   }
   /**
    * Add the bindings to the ctx.
    * 
    * @see #setBindings(Map)
    * @see #setCtx(InitialContext)
    * @throws NamingException
    */
   public void start()
      throws NamingException
   {
      log.debug("start");
      if(ctx != null)
      {
         if(bindings != null)
         {
            for(Map.Entry<String, ?> entry : bindings.entrySet())
            {
               String key = entry.getKey();
               Object value = entry.getValue();
               log.debug("Binding, key="+key+", value="+value+", type="+value.getClass());
               Util.bind(ctx, key, value);
            }
         }
      }
   }
   /**
    * Remove the bindings from the ctx.
    * @throws NamingException
    */
   public void stop()
      throws NamingException
   {
      if(ctx != null)
      {
         if(bindings != null)
         {
            for(String key : bindings.keySet())
            {
               Util.unbind(ctx, key);
            }
         }
      }
   }
}
