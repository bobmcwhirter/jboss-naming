/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
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

import javax.naming.RefAddr;
import javax.naming.StringRefAddr;
import javax.naming.Reference;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Map;
import java.util.Hashtable;

/**
 * Bean that initializes the "java:comp" context
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @version $Revision: 1.1 $
 */
public class JavaCompInitializer
{
   private InitialContext iniCtx;
   private Hashtable initialContextProperties;

   public Hashtable getInitialContextProperties()
   {
      return initialContextProperties;
   }

   public void setInitialContextProperties(Hashtable initialContextProperties)
   {
      this.initialContextProperties = initialContextProperties;
   }

   public InitialContext getIniCtx()
   {
      return iniCtx;
   }

   public void setIniCtx(InitialContext iniCtx)
   {
      this.iniCtx = iniCtx;
   }

   protected void initialContext() throws Exception
   {
      if (iniCtx != null) return;
      if (initialContextProperties == null)
      {
         iniCtx = new InitialContext();
      }
      else
      {
         iniCtx = new InitialContext(initialContextProperties);
      }
   }

   public void start() throws Exception
   {
      initialContext();
      ClassLoader topLoader = Thread.currentThread().getContextClassLoader();
      ENCFactory.setTopClassLoader(topLoader);
      RefAddr refAddr = new StringRefAddr("nns", "ENC");
      Reference envRef = new Reference("javax.naming.Context", refAddr, ENCFactory.class.getName(), null);
      Context ctx = (Context)iniCtx.lookup("java:");
      ctx.rebind("comp", envRef);
   }
}
