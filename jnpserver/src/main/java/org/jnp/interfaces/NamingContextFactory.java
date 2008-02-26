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

import java.util.Hashtable;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.*;

/** The jnp naming provider InitialContextFactory implementation.

@see javax.naming.spi.InitialContextFactory

@author Scott.Stark@jboss.org
@version $Revision$
 */
public class NamingContextFactory
   implements InitialContextFactory, ObjectFactory
{
    // InitialContextFactory implementation --------------------------
    public Context getInitialContext(Hashtable env) 
      throws NamingException
    {
        String providerURL = (String) env.get(Context.PROVIDER_URL);
        Name prefix = null;
        /** This may be a comma separated list of provider urls in which
          case we do not parse the urls for the requested context prefix name
        */
        int comma = providerURL != null ? providerURL.indexOf(',') : -1;
        if( providerURL != null && comma < 0 )
        {
            Name name = new CompoundName(providerURL, NamingParser.syntax);
            String serverInfo = NamingContext.parseNameForScheme(name, env);
            if( serverInfo != null )
            {
               env = (Hashtable) env.clone();
               // Set hostname:port value for the naming server
               env.put(Context.PROVIDER_URL, serverInfo);
               // Set the context prefix to name
               Name parsedName = (Name) env.get(NamingContext.JNP_PARSED_NAME);
               if( parsedName != null )
                  prefix = parsedName;
               else
                  prefix = name;
            }
        }
        return new NamingContext(env, prefix, null);
    }

   // ObjectFactory implementation ----------------------------------
   public Object getObjectInstance(Object obj,
                                Name name,
                                Context nameCtx,
                                Hashtable environment)
                         throws Exception
   {
//      System.out.println(obj+" "+name+" "+nameCtx+" "+environment);
      Context ctx = getInitialContext(environment);
      Reference ref = (Reference)obj;
      return ctx.lookup((String)ref.get("URL").getContent());
   }

}
