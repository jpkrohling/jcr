/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.observation;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: ListenerCriteria.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class ListenerCriteria
{

   private int eventTypes;

   private String absPath;

   private boolean deep;

   private String[] identifier;

   private String[] nodeTypeName;

   private boolean noLocal;

   private String sessionId;

   public ListenerCriteria(int eventTypes, String absPath, boolean isDeep, String[] identifier, String[] nodeTypeName,
            boolean noLocal, String sessionId) throws RepositoryException
   {
      this.eventTypes = eventTypes;
      this.absPath = absPath;
      this.deep = isDeep;
      this.identifier = identifier;
      this.nodeTypeName = nodeTypeName;
      this.noLocal = noLocal;
      this.sessionId = sessionId;
   }

   public int getEventTypes()
   {
      return this.eventTypes;
   }

   public String getAbsPath()
   {
      return this.absPath;
   }

   public boolean isDeep()
   {
      return deep;
   }

   public String[] getIdentifier()
   {
      return this.identifier;
   }

   public String[] getNodeTypeName()
   {
      return this.nodeTypeName;
   }

   public boolean getNoLocal()
   {
      return this.noLocal;
   }

   public String getSessionId()
   {
      return this.sessionId;
   }

}
