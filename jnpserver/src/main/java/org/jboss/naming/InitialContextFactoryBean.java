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

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * A bean that creates an InitialContext based on its env Properties
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class InitialContextFactoryBean
{
   private Properties env;
   private InitialContext ctx;

   public Properties getEnv()
   {
      return env;
   }
   public void setEnv(Properties env)
   {
      this.env = env;
   }

   public synchronized InitialContext getCtx()
      throws NamingException
   {
      System.out.println(env);
      if(ctx == null)
         ctx = new InitialContext(env);
      return ctx;
   }
}
