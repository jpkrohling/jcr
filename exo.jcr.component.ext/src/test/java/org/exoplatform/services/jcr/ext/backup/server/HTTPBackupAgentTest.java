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
package org.exoplatform.services.jcr.ext.backup.server;

import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.backup.AbstractBackupTestCase;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.BackupJob;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.ContainerRequestUserRole;
import org.exoplatform.services.jcr.ext.backup.ExtendedBackupManager;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.impl.JobRepositoryRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobWorkspaceRestore;
import org.exoplatform.services.jcr.ext.backup.server.bean.BackupConfigBean;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.BackupServiceInfoBean;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.DetailedInfo;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.ShortInfo;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.ShortInfoList;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.rest.RequestHandler;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.InputHeadersMap;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.impl.ResourceBinder;
import org.exoplatform.services.rest.tools.ByteArrayContainerResponseWriter;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.ws.frameworks.json.JsonHandler;
import org.exoplatform.ws.frameworks.json.JsonParser;
import org.exoplatform.ws.frameworks.json.JsonWriter;
import org.exoplatform.ws.frameworks.json.impl.BeanBuilder;
import org.exoplatform.ws.frameworks.json.impl.JsonDefaultHandler;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;
import org.exoplatform.ws.frameworks.json.impl.JsonWriterImpl;
import org.exoplatform.ws.frameworks.json.value.JsonValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 21.04.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: HTTPBackupAgentTest.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class HTTPBackupAgentTest
   extends AbstractBackupTestCase
{

   private String HTTP_BACKUP_AGENT_PATH = HTTPBackupAgent.Constants.BASE_URL;

   private ResourceBinder binder;

   private RequestHandler handler;

   /**
    * {@inheritDoc}
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      binder = (ResourceBinder) container.getComponentInstanceOfType(ResourceBinder.class);
      handler = (RequestHandler) container.getComponentInstanceOfType(RequestHandler.class);

      SessionProviderService sessionProviderService =
               (SessionProviderService) container.getComponentInstanceOfType(ThreadLocalSessionProviderService.class);
      assertNotNull(sessionProviderService);
      sessionProviderService.setSessionProvider(null, new SessionProvider(new ConversationState(new Identity("root"))));
   }

   public void testInfo() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.BACKUP_SERVICE_INFO), new URI(""), null,
                        new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      BackupServiceInfoBean info =
               (BackupServiceInfoBean) getObject(BackupServiceInfoBean.class, responseWriter.getBody());
      BackupManager backupManager = (BackupManager) container.getComponentInstanceOfType(BackupManager.class);

      assertNotNull(info);
      assertEquals(backupManager.getBackupDirectory().getAbsolutePath(), info.getBackupLogDir());
      assertEquals(backupManager.getFullBackupType(), info.getFullBackupType());
      assertEquals(backupManager.getIncrementalBackupType(), info.getIncrementalBackupType());
      assertEquals(backupManager.getDefaultIncrementalJobPeriod(), info.getDefaultIncrementalJobPeriod().longValue());
   }

   public void testDropWorkspace() throws Exception
   {
      // login to workspace '/db6/ws1'
      Session session_db6_ws1 = repositoryService.getRepository("db6").login(credentials, "ws1");

      assertNotNull(session_db6_ws1);

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.DROP_WORKSPACE + "/db6/ws1/true"), new URI(""), null,
                        new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      try
      {
         Session ses_db6_ws1 = repositoryService.getRepository("db6").login(credentials, "ws1");
         fail();
      }
      catch (NoSuchWorkspaceException e)
      {
         // ok
      }
   }

   public void testStart() throws Exception
   {
      // login to workspace '/db6/ws2'
      Session session_db6_ws2 = repositoryService.getRepository("db6").login(credentials, "ws2");
      assertNotNull(session_db6_ws2);

      session_db6_ws2.getRootNode().addNode("NODE_NAME_TO_TEST");
      session_db6_ws2.save();

      File f = new File("target/temp/backup/" + System.currentTimeMillis());
      f.mkdirs();

      BackupConfigBean configBean = new BackupConfigBean(BackupManager.FULL_AND_INCREMENTAL, f.getPath(), 10000l);

      JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
      JsonValue json = generatorImpl.createJsonObject(configBean);

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-Type", "application/json; charset=UTF-8");
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("POST", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.START_BACKUP + "/db6/ws2"), new URI(""),
                        new ByteArrayInputStream(json.toString().getBytes("UTF-8")), new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      Thread.sleep(5000);
   }

   public void testStartBackupRepository() throws Exception
   {
      Session session_db6_ws2 = repositoryService.getRepository("db6").login(credentials, "ws2");
      assertNotNull(session_db6_ws2);

      session_db6_ws2.getRootNode().addNode("NODE_NAME_TO_TEST");
      session_db6_ws2.save();

      File f = new File("target/temp/backup/" + System.currentTimeMillis());
      f.mkdirs();

      BackupConfigBean configBean = new BackupConfigBean(BackupManager.FULL_AND_INCREMENTAL, f.getPath(), 10000l);

      JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
      JsonValue json = generatorImpl.createJsonObject(configBean);

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-Type", "application/json; charset=UTF-8");
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("POST", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.START_BACKUP_REPOSITORY + "/db6"), new URI(""),
                        new ByteArrayInputStream(json.toString().getBytes("UTF-8")), new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      Thread.sleep(10000);
   }

   public void testInfoBackup() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.CURRENT_AND_COMPLETED_BACKUPS_INFO), new URI(""),
                        null, new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      assertEquals(1, list.size());

      ShortInfo info = list.get(0);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, info.getType().intValue());
      assertEquals(BackupJob.FINISHED, info.getState().intValue());
      assertEquals("db6", info.getRepositoryName());
      assertEquals("ws2", info.getWorkspaceName());
   }

   public void testInfoBackupRepository() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.CURRENT_AND_COMPLETED_BACKUPS_REPOSITORY_INFO),
                        new URI(""), null, new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      assertEquals(1, list.size());

      ShortInfo info = list.get(0);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, info.getType().intValue());
      assertTrue(RepositoryBackupChain.WORKING == info.getState().intValue()
               || RepositoryBackupChain.FULL_BACKUP_FINISHED_INCREMENTAL_BACKUP_WORKING == info.getState().intValue());
      assertEquals("db6", info.getRepositoryName());
   }

   public void testInfoBackupOnWorkspace() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET",
                        new URI(HTTP_BACKUP_AGENT_PATH
                                 + HTTPBackupAgent.Constants.OperationType.CURRENT_AND_COMPLETED_BACKUPS_INFO_ON_WS
                                 + "/db6/ws2"), new URI(""), null, new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      assertEquals(1, list.size());

      ShortInfo info = list.get(0);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, info.getType().intValue());
      assertEquals(BackupJob.FINISHED, info.getState().intValue());
      assertEquals("db6", info.getRepositoryName());
      assertEquals("ws2", info.getWorkspaceName());
   }

   public void testInfoBackupOnRepository() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.CURRENT_AND_COMPLETED_BACKUPS_REPOSITORY_INFO
                        + "/db6"), new URI(""), null, new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      assertEquals(1, list.size());

      ShortInfo info = list.get(0);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, info.getType().intValue());
      assertTrue(RepositoryBackupChain.WORKING == info.getState().intValue()
               || RepositoryBackupChain.FULL_BACKUP_FINISHED_INCREMENTAL_BACKUP_WORKING == info.getState().intValue());
   }

   public void testInfoBackupCurrent() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.CURRENT_BACKUPS_INFO), new URI(""), null,
                        new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      assertEquals(1, list.size());

      ShortInfo info = list.get(0);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, info.getType().intValue());
      assertEquals(BackupJob.FINISHED, info.getState().intValue());
      assertEquals("db6", info.getRepositoryName());
      assertEquals("ws2", info.getWorkspaceName());
   }

   public void testInfoBackupRepositoryCurrent() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.CURRENT_BACKUPS_REPOSITORY_INFO), new URI(""), null,
                        new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      assertEquals(1, list.size());

      ShortInfo info = list.get(0);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, info.getType().intValue());
      assertTrue(RepositoryBackupChain.WORKING == info.getState().intValue()
               || RepositoryBackupChain.FULL_BACKUP_FINISHED_INCREMENTAL_BACKUP_WORKING == info.getState().intValue());
      assertEquals("db6", info.getRepositoryName());
   }

   public void testInfoBackupCurrentById() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_BACKUPS_INFO), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");
         assertEquals(info.getWorkspaceName(), "ws2");

         id = info.getBackupId();
      }

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.CURRENT_OR_COMPLETED_BACKUP_INFO + "/" + id),
                        new URI(""), null, new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

      assertNotNull(info);
      assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, info.getType().intValue());
      assertEquals(BackupJob.FINISHED, info.getState().intValue());
      assertEquals("db6", info.getRepositoryName());
      assertEquals("ws2", info.getWorkspaceName());
      assertNotNull(info.getBackupConfig());
   }

   public void testInfoBackupRepositoryId() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_AND_COMPLETED_BACKUPS_REPOSITORY_INFO),
                           new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");

         id = info.getBackupId();
      }

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.CURRENT_OR_COMPLETED_BACKUP_REPOSITORY_INFO + "/"
                        + id), new URI(""), null, new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

      assertNotNull(info);
      assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, info.getType().intValue());
      assertTrue(RepositoryBackupChain.WORKING == info.getState().intValue()
               || RepositoryBackupChain.FULL_BACKUP_FINISHED_INCREMENTAL_BACKUP_WORKING == info.getState().intValue());
      assertEquals("db6", info.getRepositoryName());
      assertNotNull(info.getBackupConfig());
   }

   public void testStop() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_BACKUPS_INFO), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");
         assertEquals(info.getWorkspaceName(), "ws2");

         id = info.getBackupId();
      }

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.STOP_BACKUP + "/" + id), new URI(""), null,
                        new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());
   }

   public void testStopBackupRepository() throws Exception
   {
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_BACKUPS_REPOSITORY_INFO), new URI(""),
                           null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");

         id = info.getBackupId();
      }

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.STOP_BACKUP_REPOSITORY + "/" + id), new URI(""),
                        null, new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());
   }

   public void testInfoBackupCompleted() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO), new URI(""), null,
                        new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      assertEquals(1, list.size());

      ShortInfo info = list.get(0);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.COMPLETED, info.getType().intValue());
      assertEquals(0, info.getState().intValue());
      assertEquals("db6", info.getRepositoryName());
      assertEquals("ws2", info.getWorkspaceName());
   }

   public void testInfoBackupRepositoryCompleted() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_REPOSITORY_INFO), new URI(""),
                        null, new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      assertEquals(1, list.size());

      ShortInfo info = list.get(0);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.COMPLETED, info.getType().intValue());
      assertEquals(0, info.getState().intValue());
      assertEquals("db6", info.getRepositoryName());
   }

   public void testInfoBackupCompletedById() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");
         assertEquals(info.getWorkspaceName(), "ws2");

         id = info.getBackupId();
      }

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.CURRENT_OR_COMPLETED_BACKUP_INFO + "/" + id),
                        new URI(""), null, new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

      assertNotNull(info);
      assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.COMPLETED, info.getType().intValue());
      assertEquals(0, info.getState().intValue());
      assertEquals("db6", info.getRepositoryName());
      assertEquals("ws2", info.getWorkspaceName());

      assertNotNull(info.getBackupConfig());
   }

   public void testGetDefaultWorkspaceConfig() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_WORKSPACE_CONFIG), new URI(""), null,
                        new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      WorkspaceEntry defEntry = (WorkspaceEntry) getObject(WorkspaceEntry.class, responseWriter.getBody());

      assertEquals(repository.getConfiguration().getDefaultWorkspaceName(), defEntry.getName());
   }

   public void testGetDefaultRepositoryConfig() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
               new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                        + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_REPOSITORY_CONFIG), new URI(""), null,
                        new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      RepositoryEntry defEntry = (RepositoryEntry) getObject(RepositoryEntry.class, responseWriter.getBody());

      assertEquals(repository.getConfiguration().getName(), defEntry.getName());
   }

   public void testWorkspaceEntryRestore() throws Exception
   {
      // Getting default WorkspaceEntry
      WorkspaceEntry defEntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_WORKSPACE_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defEntry = (WorkspaceEntry) getObject(WorkspaceEntry.class, responseWriter.getBody());
      }

      WorkspaceEntry wEntry = makeWorkspaceEntry(defEntry, "db6", "ws3", "jdbcjcr24");

      // Restore

      // Create JSON to WorkspaceEntry
      JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
      JsonValue json = generatorImpl.createJsonObject(wEntry);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      JsonWriter jsonWriter = new JsonWriterImpl(out);
      json.writeTo(jsonWriter);
      jsonWriter.flush();
      jsonWriter.close();

      // Create WorkspaceEntry from JSON
      ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
      JsonParser jsonParser = new JsonParserImpl();
      JsonHandler jsonHandler = new JsonDefaultHandler();

      jsonParser.parse(in, jsonHandler);
      JsonValue jsonValue = jsonHandler.getJsonObject();

      WorkspaceEntry entry = (WorkspaceEntry) (new BeanBuilder().createObject(WorkspaceEntry.class, jsonValue));

      assertNotNull(entry);

   }

   public void testRestore() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");
         assertEquals(info.getWorkspaceName(), "ws2");

         id = info.getBackupId();
      }

      // Getting default WorkspaceEntry
      WorkspaceEntry defEntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_WORKSPACE_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defEntry = (WorkspaceEntry) getObject(WorkspaceEntry.class, responseWriter.getBody());
      }

      WorkspaceEntry wEntry = makeWorkspaceEntry(defEntry, "db6", "ws3", "jdbcjcr24");

      // Check the workspace /db6/ws3 not exists.
      try
      {
         Session sessin_ws3 = repositoryService.getRepository("db6").login(credentials, "ws3");
         fail("The workspace /db6/ws3 should not exists.");
      }
      catch (Exception e)
      {
         // ok
      }

      // Restore
      {
         // Create JSON to WorkspaceEntry
         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json = generatorImpl.createJsonObject(wEntry);

         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         headers.putSingle("Content-Type", "application/json; charset=UTF-8");
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("POST", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE + "/" + "db6" + "/" + id), new URI(""),
                           new ByteArrayInputStream(json.toString().getBytes("UTF-8")), new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitWorkspaceRestore("db6", "ws3");

      // Get restore info to workspace /db6/ws3
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + "db6" + "/"
                           + "ws3"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertEquals("ws3", info.getWorkspaceName());
         assertNotNull(info.getBackupConfig());

         Session sessin_ws3 = repositoryService.getRepository("db6").login(credentials, "ws3");
         assertNotNull(sessin_ws3);
         assertNotNull(sessin_ws3.getRootNode());
      }

      // Get restores info
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORES), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         assertNotNull(infoList);

         ShortInfo info = new ArrayList<ShortInfo>(infoList.getBackups()).get(0);

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertEquals("ws3", info.getWorkspaceName());
         assertNotNull(info.getBackupId());

         Session sessin_ws3 = repositoryService.getRepository("db6").login(credentials, "ws3");
         assertNotNull(sessin_ws3);
         assertNotNull(sessin_ws3.getRootNode());
      }
   }

   public void testRestoreExistsTrue() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");
         assertEquals(info.getWorkspaceName(), "ws2");

         id = info.getBackupId();
      }

      // Getting default WorkspaceEntry
      WorkspaceEntry defEntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_WORKSPACE_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defEntry = (WorkspaceEntry) getObject(WorkspaceEntry.class, responseWriter.getBody());
      }

      WorkspaceEntry wEntry = makeWorkspaceEntry(defEntry, "db6", "ws3", "jdbcjcr24");

      // Check the workspace /db6/ws3 is exists.
      try
      {
         Session sessin_ws3 = repositoryService.getRepository("db6").login(credentials, "ws3");
         // ok  
      }
      catch (Exception e)
      {
         fail("The workspace /db6/ws3 should be exists.");
      }

      // Restore
      {
         // Create JSON to WorkspaceEntry
         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json = generatorImpl.createJsonObject(wEntry);

         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         headers.putSingle("Content-Type", "application/json; charset=UTF-8");
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("POST", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE + "/" + "db6" + "/" + id + "/" + "true"),
                           new URI(""), new ByteArrayInputStream(json.toString().getBytes("UTF-8")),
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitWorkspaceRestore("db6", "ws3");

      // Get restore info to workspace /db6/ws3
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + "db6" + "/"
                           + "ws3"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertEquals("ws3", info.getWorkspaceName());
         assertNotNull(info.getBackupConfig());

         Session sessin_ws3 = repositoryService.getRepository("db6").login(credentials, "ws3");
         assertNotNull(sessin_ws3);
         assertNotNull(sessin_ws3.getRootNode());
      }
   }

   public void testRestoreExistsFalse() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");
         assertEquals(info.getWorkspaceName(), "ws2");

         id = info.getBackupId();
      }

      // Getting default WorkspaceEntry
      WorkspaceEntry defEntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_WORKSPACE_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defEntry = (WorkspaceEntry) getObject(WorkspaceEntry.class, responseWriter.getBody());
      }

      WorkspaceEntry wEntry = makeWorkspaceEntry(defEntry, "db6", "ws3", "jdbcjcr24");

      removeWorkspaceFully("db6", "ws3");

      // Check the workspace /db6/ws3 is not exists.
      try
      {
         Session sessin_ws3 = repositoryService.getRepository("db6").login(credentials, "ws3");
         fail("The workspace /db6/ws3 should  be not exists.");
      }
      catch (Exception e)
      {
         // ok
      }

      // Restore
      {
         // Create JSON to WorkspaceEntry
         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json = generatorImpl.createJsonObject(wEntry);

         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         headers.putSingle("Content-Type", "application/json; charset=UTF-8");
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("POST", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE + "/" + "db6" + "/" + id + "/" + "false"),
                           new URI(""), new ByteArrayInputStream(json.toString().getBytes("UTF-8")),
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitWorkspaceRestore("db6", "ws3");

      // Get restore info to workspace /db6/ws3
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + "db6" + "/"
                           + "ws3"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertEquals("ws3", info.getWorkspaceName());
         assertNotNull(info.getBackupConfig());

         Session sessin_ws3 = repositoryService.getRepository("db6").login(credentials, "ws3");
         assertNotNull(sessin_ws3);
         assertNotNull(sessin_ws3.getRootNode());
      }
   }

   public void testRestoreExistsByIdOriginalConfigTrue() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");
         assertEquals(info.getWorkspaceName(), "ws2");

         id = info.getBackupId();
      }

      // Check the workspace /db6/ws3 is exists.
      try
      {
         Session sessin_ws2 = repositoryService.getRepository("db6").login(credentials, "ws2");
         // ok  
      }
      catch (Exception e)
      {
         fail("The workspace /db6/ws2 should be exists.");
      }

      // Restore
      {
         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE + "/" + id + "/" + "true"), new URI(""),
                           null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitWorkspaceRestore("db6", "ws2");

      // Get restore info to workspace /db6/ws2
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + "db6" + "/"
                           + "ws2"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertEquals("ws2", info.getWorkspaceName());
         assertNotNull(info.getBackupConfig());

         Session sessin_ws2 = repositoryService.getRepository("db6").login(credentials, "ws2");
         assertNotNull(sessin_ws2);
         assertNotNull(sessin_ws2.getRootNode());
      }
   }

   public void testRestoreExistsByIdOriginalConfigFalse() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");
         assertEquals(info.getWorkspaceName(), "ws2");

         id = info.getBackupId();
      }

      removeWorkspaceFully("db6", "ws2");

      // Check the workspace /db6/ws2 is exists.
      try
      {
         Session sessin_ws2 = repositoryService.getRepository("db6").login(credentials, "ws2");
         fail("The workspace /db6/ws2 should be not exists.");
      }
      catch (Exception e)
      {
         // ok  
      }

      // Restore
      {
         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE + "/" + id + "/" + "false"), new URI(""),
                           null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitWorkspaceRestore("db6", "ws2");

      // Get restore info to workspace /db6/ws2
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + "db6" + "/"
                           + "ws2"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertEquals("ws2", info.getWorkspaceName());
         assertNotNull(info.getBackupConfig());

         Session sessin_ws2 = repositoryService.getRepository("db6").login(credentials, "ws2");
         assertNotNull(sessin_ws2);
         assertNotNull(sessin_ws2.getRootNode());
      }
   }

   public void testRestoreBackupSetExistsTrue() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;
      String backupSetPath = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");
         assertEquals(info.getWorkspaceName(), "ws2");

         id = info.getBackupId();

         for (BackupChainLog bcl : backup.getBackupsLogs())
         {
            if (bcl.getBackupId().equals(id))
            {
               backupSetPath = bcl.getBackupConfig().getBackupDir().getCanonicalPath();
               break;
            }
         }
      }

      // Getting default WorkspaceEntry
      WorkspaceEntry defEntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_WORKSPACE_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defEntry = (WorkspaceEntry) getObject(WorkspaceEntry.class, responseWriter.getBody());
      }

      WorkspaceEntry wEntry = makeWorkspaceEntry(defEntry, "db6", "ws3", "jdbcjcr24");

      // Check the workspace /db6/ws3 is exists.
      try
      {
         Session sessin_ws3 = repositoryService.getRepository("db6").login(credentials, "ws3");
         // ok  
      }
      catch (Exception e)
      {
         fail("The workspace /db6/ws3 should be exists.");
      }

      // Restore
      {
         // Create JSON to WorkspaceEntry
         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json = generatorImpl.createJsonObject(wEntry);

         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         headers.putSingle("Content-Type", "application/json; charset=UTF-8");
         ContainerRequestUserRole creq =
            new ContainerRequestUserRole("POST", new URI(HTTP_BACKUP_AGENT_PATH
               + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET + "/" + "db6" + "/" + backupSetPath + "/"
               + "true"), new URI(""), new ByteArrayInputStream(json.toString().getBytes("UTF-8")),
               new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitWorkspaceRestore("db6", "ws3");

      // Get restore info to workspace /db6/ws3
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + "db6" + "/"
                           + "ws3"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertEquals("ws3", info.getWorkspaceName());
         assertNotNull(info.getBackupConfig());

         Session sessin_ws3 = repositoryService.getRepository("db6").login(credentials, "ws3");
         assertNotNull(sessin_ws3);
         assertNotNull(sessin_ws3.getRootNode());
      }
   }

   public void testRestoreOriginalConfigBackupSetExistsTrue() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;
      String backupSetPath = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");
         assertEquals(info.getWorkspaceName(), "ws2");

         id = info.getBackupId();

         for (BackupChainLog bcl : backup.getBackupsLogs())
         {
            if (bcl.getBackupId().equals(id))
            {
               backupSetPath = bcl.getBackupConfig().getBackupDir().getCanonicalPath();
               break;
            }
         }
      }

      // Check the workspace /db6/ws2 is exists.
      try
      {
         Session sessin_ws2 = repositoryService.getRepository("db6").login(credentials, "ws2");
         // ok  
      }
      catch (Exception e)
      {
         fail("The workspace /db6/ws2 should be exists.");
      }

      // Restore
      {
         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET + "/" + backupSetPath + "/"
                           + "true"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitWorkspaceRestore("db6", "ws2");

      // Get restore info to workspace /db6/ws2
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + "db6" + "/"
                           + "ws2"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertEquals("ws2", info.getWorkspaceName());
         assertNotNull(info.getBackupConfig());

         Session sessin_ws2 = repositoryService.getRepository("db6").login(credentials, "ws2");
         assertNotNull(sessin_ws2);
         assertNotNull(sessin_ws2.getRootNode());
      }
   }

   public void testRestoreOriginalConfigBackupSetExistsFalse() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;
      String backupSetPath = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");
         assertEquals(info.getWorkspaceName(), "ws2");

         id = info.getBackupId();

         for (BackupChainLog bcl : backup.getBackupsLogs())
         {
            if (bcl.getBackupId().equals(id))
            {
               backupSetPath = bcl.getBackupConfig().getBackupDir().getCanonicalPath();
               break;
            }
         }
      }

      removeWorkspaceFully("db6", "ws2");
      // Check the workspace /db6/ws2 is exists.
      try
      {
         Session sessin_ws2 = repositoryService.getRepository("db6").login(credentials, "ws2");
         fail("The workspace /db6/ws2 should be not exists.");
      }
      catch (Exception e)
      {
         // ok
      }

      // Restore
      {
         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET + "/" + backupSetPath + "/"
                           + "false"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitWorkspaceRestore("db6", "ws2");

      // Get restore info to workspace /db6/ws2
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + "db6" + "/"
                           + "ws2"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertEquals("ws2", info.getWorkspaceName());
         assertNotNull(info.getBackupConfig());

         Session sessin_ws2 = repositoryService.getRepository("db6").login(credentials, "ws2");
         assertNotNull(sessin_ws2);
         assertNotNull(sessin_ws2.getRootNode());
      }
   }

   public void testRestoreBackupSetExistsFalse() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;
      String backupSetPath = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");
         assertEquals(info.getWorkspaceName(), "ws2");

         id = info.getBackupId();

         for (BackupChainLog bcl : backup.getBackupsLogs())
         {
            if (bcl.getBackupId().equals(id))
            {
               backupSetPath = bcl.getBackupConfig().getBackupDir().getCanonicalPath();
               break;
            }
         }
      }

      // Getting default WorkspaceEntry
      WorkspaceEntry defEntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_WORKSPACE_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defEntry = (WorkspaceEntry) getObject(WorkspaceEntry.class, responseWriter.getBody());
      }

      WorkspaceEntry wEntry = makeWorkspaceEntry(defEntry, "db6", "ws3", "jdbcjcr24");

      removeWorkspaceFully("db6", "ws3");

      // Check the workspace /db6/ws3 is not exists.
      try
      {
         Session sessin_ws3 = repositoryService.getRepository("db6").login(credentials, "ws3");
         fail("The workspace /db6/ws3 should be not exists.");
      }
      catch (Exception e)
      {
         // ok  
      }

      // Restore
      {
         // Create JSON to WorkspaceEntry
         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json = generatorImpl.createJsonObject(wEntry);

         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         headers.putSingle("Content-Type", "application/json; charset=UTF-8");
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("POST", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET + "/" + "db6" + "/"
                           + backupSetPath + "/" + "false"), new URI(""), new ByteArrayInputStream(json.toString()
                           .getBytes("UTF-8")), new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitWorkspaceRestore("db6", "ws3");

      // Get restore info to workspace /db6/ws3
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + "db6" + "/"
                           + "ws3"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertEquals("ws3", info.getWorkspaceName());
         assertNotNull(info.getBackupConfig());

         Session sessin_ws3 = repositoryService.getRepository("db6").login(credentials, "ws3");
         assertNotNull(sessin_ws3);
         assertNotNull(sessin_ws3.getRootNode());
      }
   }

   public void testRestoreRepository() throws Exception
   {
      // Get backup id for backup on workspace /db6/ws2
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_REPOSITORY_INFO), new URI(""),
                           null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");

         id = info.getBackupId();
      }

      // Getting default RepositoryEntry
      RepositoryEntry defREntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_REPOSITORY_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defREntry = (RepositoryEntry) getObject(RepositoryEntry.class, responseWriter.getBody());
      }

      // Getting default WorkspaceEntry
      WorkspaceEntry defWEntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_WORKSPACE_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defWEntry = (WorkspaceEntry) getObject(WorkspaceEntry.class, responseWriter.getBody());
      }

      ArrayList<WorkspaceEntry> wsEntries = new ArrayList<WorkspaceEntry>();
      wsEntries.add(makeWorkspaceEntry(defWEntry, "db6backup", "ws", "jdbcjcr27"));
      wsEntries.add(makeWorkspaceEntry(defWEntry, "db6backup", "ws2", "jdbcjcr27"));
      RepositoryEntry rEntry = makeRepositoryEntry(defREntry, "db6backup", wsEntries);

      // Check the repository /db6backup.
      try
      {
         repositoryService.getRepository("db6backup");
         fail("The repository /db6backup should be not exists.");
      }
      catch (Exception e)
      {
         // ok
      }

      // Restore
      {
         // Create JSON to WorkspaceEntry
         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json = generatorImpl.createJsonObject(rEntry);

         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         headers.putSingle("Content-Type", "application/json; charset=UTF-8");
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("POST", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY + "/" + id), new URI(""),
                           new ByteArrayInputStream(json.toString().getBytes("UTF-8")), new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitRepositoryRestore("db6backup");

      // Get restore info
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/"
                           + "db6backup"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6backup", info.getRepositoryName());
         assertNotNull(info.getBackupConfig());

         assertNotNull(repositoryService.getRepository("db6backup"));
         Session sessin_ws = repositoryService.getRepository("db6backup").login(credentials, "ws");
         assertNotNull(sessin_ws);
         assertNotNull(sessin_ws.getRootNode());
      }

      // Get restores info
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORES_REPOSITORY), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         assertNotNull(infoList);

         ShortInfo info = new ArrayList<ShortInfo>(infoList.getBackups()).get(0);

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6backup", info.getRepositoryName());
         assertNotNull(info.getBackupId());

         assertNotNull(repositoryService.getRepository("db6backup"));
         Session sessin_ws = repositoryService.getRepository("db6backup").login(credentials, "ws");
         assertNotNull(sessin_ws);
         assertNotNull(sessin_ws.getRootNode());
      }
   }

   public void testRestoreRepositoryExistsTrue() throws Exception
   {
      // Get backup id for backup on workspace /db6
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_REPOSITORY_INFO), new URI(""),
                           null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");

         id = info.getBackupId();
      }

      // Getting default RepositoryEntry
      RepositoryEntry defREntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_REPOSITORY_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defREntry = (RepositoryEntry) getObject(RepositoryEntry.class, responseWriter.getBody());
      }

      // Getting default WorkspaceEntry
      WorkspaceEntry defWEntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_WORKSPACE_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defWEntry = (WorkspaceEntry) getObject(WorkspaceEntry.class, responseWriter.getBody());
      }

      ArrayList<WorkspaceEntry> wsEntries = new ArrayList<WorkspaceEntry>();
      wsEntries.add(makeWorkspaceEntry(defWEntry, "db6backup", "ws", "jdbcjcr27"));
      wsEntries.add(makeWorkspaceEntry(defWEntry, "db6backup", "ws2", "jdbcjcr27"));
      RepositoryEntry rEntry = makeRepositoryEntry(defREntry, "db6backup", wsEntries);

      // Check the repository /db6backup.
      try
      {
         repositoryService.getRepository("db6backup");
         // ok
      }
      catch (Exception e)
      {
         fail("The repository /db6backup should be exists.");
      }

      // Restore
      {
         // Create JSON to WorkspaceEntry
         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json = generatorImpl.createJsonObject(rEntry);

         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         headers.putSingle("Content-Type", "application/json; charset=UTF-8");
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("POST", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY + "/" + id + "/" + "true"),
                           new URI(""),
                           new ByteArrayInputStream(json.toString().getBytes("UTF-8")), new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitRepositoryRestore("db6backup");

      // Get restore info
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/"
                           + "db6backup"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6backup", info.getRepositoryName());
         assertNotNull(info.getBackupConfig());

         assertNotNull(repositoryService.getRepository("db6backup"));
         Session sessin_ws = repositoryService.getRepository("db6backup").login(credentials, "ws");
         assertNotNull(sessin_ws);
         assertNotNull(sessin_ws.getRootNode());
      }
   }

   public void testRestoreRepositoryExistsFalse() throws Exception
   {
      // Get backup id for backup on workspace /db6
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_REPOSITORY_INFO), new URI(""),
                           null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");

         id = info.getBackupId();
      }

      // Getting default RepositoryEntry
      RepositoryEntry defREntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_REPOSITORY_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defREntry = (RepositoryEntry) getObject(RepositoryEntry.class, responseWriter.getBody());
      }

      // Getting default WorkspaceEntry
      WorkspaceEntry defWEntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_WORKSPACE_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defWEntry = (WorkspaceEntry) getObject(WorkspaceEntry.class, responseWriter.getBody());
      }

      ArrayList<WorkspaceEntry> wsEntries = new ArrayList<WorkspaceEntry>();
      wsEntries.add(makeWorkspaceEntry(defWEntry, "db6backup", "ws", "jdbcjcr27"));
      wsEntries.add(makeWorkspaceEntry(defWEntry, "db6backup", "ws2", "jdbcjcr27"));
      RepositoryEntry rEntry = makeRepositoryEntry(defREntry, "db6backup", wsEntries);

      removeRepositoryFully("db6backup");
      // Check the repository /db6backup.
      try
      {
         repositoryService.getRepository("db6backup");
         fail("The repository /db6backup should be not exists.");
      }
      catch (Exception e)
      {
         // ok
      }

      // Restore
      {
         // Create JSON to WorkspaceEntry
         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json = generatorImpl.createJsonObject(rEntry);

         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         headers.putSingle("Content-Type", "application/json; charset=UTF-8");
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("POST", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY + "/" + id + "/" + "false"),
                           new URI(""), new ByteArrayInputStream(json.toString().getBytes("UTF-8")),
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitRepositoryRestore("db6backup");

      // Get restore info
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/"
                           + "db6backup"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6backup", info.getRepositoryName());
         assertNotNull(info.getBackupConfig());

         assertNotNull(repositoryService.getRepository("db6backup"));
         Session sessin_ws = repositoryService.getRepository("db6backup").login(credentials, "ws");
         assertNotNull(sessin_ws);
         assertNotNull(sessin_ws.getRootNode());
      }
   }

   public void testRestoreRepositoryByIdOriginalConfigExistsTrue() throws Exception
   {
      // Get backup id for backup on workspace /db6
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_REPOSITORY_INFO), new URI(""),
                           null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");

         id = info.getBackupId();
      }

      // Check the repository /db6backup.
      try
      {
         repositoryService.getRepository("db6");
         // ok
      }
      catch (Exception e)
      {
         fail("The repository /db6 should be exists.");
      }

      // Restore
      {
         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY + "/" + id + "/" + "true"),
                           new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitRepositoryRestore("db6");

      // Get restore info
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + "db6"),
                           new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertNotNull(info.getBackupConfig());

         assertNotNull(repositoryService.getRepository("db6"));
         Session sessin_ws = repositoryService.getRepository("db6").login(credentials, "ws");
         assertNotNull(sessin_ws);
         assertNotNull(sessin_ws.getRootNode());
      }
   }

   public void testRestoreRepositoryByIdOriginalConfigExistsFalse() throws Exception
   {
      // Get backup id for backup on workspace /db6
      String id = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_REPOSITORY_INFO), new URI(""),
                           null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");

         id = info.getBackupId();
      }

      removeRepositoryFully("db6");
      // Check the repository /db6backup.
      try
      {
         repositoryService.getRepository("db6");
         fail("The repository /db6 should be not exists.");
      }
      catch (Exception e)
      {
         // ok
      }

      // Restore
      {
         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY + "/" + id + "/" + "false"),
                           new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitRepositoryRestore("db6");

      // Get restore info
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + "db6"),
                           new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertNotNull(info.getBackupConfig());

         assertNotNull(repositoryService.getRepository("db6"));
         Session sessin_ws = repositoryService.getRepository("db6").login(credentials, "ws");
         assertNotNull(sessin_ws);
         assertNotNull(sessin_ws.getRootNode());
      }
   }

   public void testRestoreRepositoryChangeConfigBackupSetExistsTrue() throws Exception
   {
      // Get backup id for backup on workspace /db6
      String id = null;
      String backupSetPath = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_REPOSITORY_INFO), new URI(""),
                           null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");

         id = info.getBackupId();

         for (RepositoryBackupChainLog bcl : backup.getRepositoryBackupsLogs())
         {
            if (bcl.getBackupId().equals(id))
            {
               backupSetPath = bcl.getBackupConfig().getBackupDir().getCanonicalPath();
               break;
            }
         }
      }

      // Getting default RepositoryEntry
      RepositoryEntry defREntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_REPOSITORY_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defREntry = (RepositoryEntry) getObject(RepositoryEntry.class, responseWriter.getBody());
      }

      // Getting default WorkspaceEntry
      WorkspaceEntry defWEntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_WORKSPACE_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defWEntry = (WorkspaceEntry) getObject(WorkspaceEntry.class, responseWriter.getBody());
      }

      ArrayList<WorkspaceEntry> wsEntries = new ArrayList<WorkspaceEntry>();
      wsEntries.add(makeWorkspaceEntry(defWEntry, "db6backup", "ws", "jdbcjcr27"));
      wsEntries.add(makeWorkspaceEntry(defWEntry, "db6backup", "ws2", "jdbcjcr27"));
      RepositoryEntry rEntry = makeRepositoryEntry(defREntry, "db6backup", wsEntries);

      // Check the repository /db6backup.
      try
      {
         repositoryService.getRepository("db6backup");
         // ok
      }
      catch (Exception e)
      {
         fail("The repository /db6backup should be exists.");
      }

      // Restore
      {
         // Create JSON to WorkspaceEntry
         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json = generatorImpl.createJsonObject(rEntry);

         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         headers.putSingle("Content-Type", "application/json; charset=UTF-8");
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("POST", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY_BACKUP_SET + "/"
                           + backupSetPath + "/"
                           + "true"),
                           new URI(""), new ByteArrayInputStream(json.toString().getBytes("UTF-8")),
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitRepositoryRestore("db6backup");

      // Get restore info
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/"
                           + "db6backup"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6backup", info.getRepositoryName());
         assertNotNull(info.getBackupConfig());

         assertNotNull(repositoryService.getRepository("db6backup"));
         Session sessin_ws = repositoryService.getRepository("db6backup").login(credentials, "ws");
         assertNotNull(sessin_ws);
         assertNotNull(sessin_ws.getRootNode());
      }
   }

   public void testRestoreRepositoryChangeConfigBackupSetExistsFalse() throws Exception
   {
      // Get backup id for backup on workspace /db6
      String id = null;
      String backupSetPath = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_REPOSITORY_INFO), new URI(""),
                           null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");

         id = info.getBackupId();

         for (RepositoryBackupChainLog bcl : backup.getRepositoryBackupsLogs())
         {
            if (bcl.getBackupId().equals(id))
            {
               backupSetPath = bcl.getBackupConfig().getBackupDir().getCanonicalPath();
               break;
            }
         }
      }

      // Getting default RepositoryEntry
      RepositoryEntry defREntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_REPOSITORY_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defREntry = (RepositoryEntry) getObject(RepositoryEntry.class, responseWriter.getBody());
      }

      // Getting default WorkspaceEntry
      WorkspaceEntry defWEntry;
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_WORKSPACE_CONFIG), new URI(""), null,
                           new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
         defWEntry = (WorkspaceEntry) getObject(WorkspaceEntry.class, responseWriter.getBody());
      }

      ArrayList<WorkspaceEntry> wsEntries = new ArrayList<WorkspaceEntry>();
      wsEntries.add(makeWorkspaceEntry(defWEntry, "db6backup", "ws", "jdbcjcr27"));
      wsEntries.add(makeWorkspaceEntry(defWEntry, "db6backup", "ws2", "jdbcjcr27"));
      RepositoryEntry rEntry = makeRepositoryEntry(defREntry, "db6backup", wsEntries);

      removeRepositoryFully("db6backup");
      // Check the repository /db6backup.
      try
      {
         repositoryService.getRepository("db6backup");
         fail("The repository /db6backup should be not exists.");
      }
      catch (Exception e)
      {
         // ok
      }

      // Restore
      {
         // Create JSON to WorkspaceEntry
         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json = generatorImpl.createJsonObject(rEntry);

         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         headers.putSingle("Content-Type", "application/json; charset=UTF-8");
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("POST", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY_BACKUP_SET + "/"
                           + backupSetPath + "/" + "false"), new URI(""), new ByteArrayInputStream(json.toString()
                           .getBytes("UTF-8")), new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitRepositoryRestore("db6backup");

      // Get restore info
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/"
                           + "db6backup"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6backup", info.getRepositoryName());
         assertNotNull(info.getBackupConfig());

         assertNotNull(repositoryService.getRepository("db6backup"));
         Session sessin_ws = repositoryService.getRepository("db6backup").login(credentials, "ws");
         assertNotNull(sessin_ws);
         assertNotNull(sessin_ws.getRootNode());
      }
   }

   public void testRestoreRepositoryBackupSetExistsTrue() throws Exception
   {
      // Get backup id for backup on workspace /db6
      String id = null;
      String backupSetPath = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_REPOSITORY_INFO), new URI(""),
                           null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");

         id = info.getBackupId();

         for (RepositoryBackupChainLog bcl : backup.getRepositoryBackupsLogs())
         {
            if (bcl.getBackupId().equals(id))
            {
               backupSetPath = bcl.getBackupConfig().getBackupDir().getCanonicalPath();
               break;
            }
         }
      }

      // Check the repository /db6.
      try
      {
         repositoryService.getRepository("db6");
         // ok
      }
      catch (Exception e)
      {
         fail("The repository /db6 should be exists.");
      }

      // Restore
      {
         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET + "/" + backupSetPath + "/"
                           + "true"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitRepositoryRestore("db6");

      // Get restore info
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + "db6"),
                           new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertNotNull(info.getBackupConfig());

         assertNotNull(repositoryService.getRepository("db6"));
         Session sessin_ws = repositoryService.getRepository("db6").login(credentials, "ws");
         assertNotNull(sessin_ws);
         assertNotNull(sessin_ws.getRootNode());
      }
   }

   public void testRestoreRepositoryBackupSetExistsFalse() throws Exception
   {
      // Get backup id for backup on workspace /db6
      String id = null;
      String backupSetPath = null;

      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_REPOSITORY_INFO), new URI(""),
                           null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         ShortInfoList infoList = (ShortInfoList) getObject(ShortInfoList.class, responseWriter.getBody());
         List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

         assertEquals(1, list.size());

         ShortInfo info = list.get(0);

         assertEquals(info.getRepositoryName(), "db6");

         id = info.getBackupId();

         for (RepositoryBackupChainLog bcl : backup.getRepositoryBackupsLogs())
         {
            if (bcl.getBackupId().equals(id))
            {
               backupSetPath = bcl.getBackupConfig().getBackupDir().getCanonicalPath();
               break;
            }
         }
      }

      removeRepositoryFully("db6");
      // Check the repository /db6.
      try
      {
         repositoryService.getRepository("db6");
         fail("The repository /db6 should be not exists.");
      }
      catch (Exception e)
      {
         // ok
      }

      // Restore
      {
         // Execute restore
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET + "/" + backupSetPath + "/"
                           + "false"), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());
      }

      waitRepositoryRestore("db6");

      // Get restore info
      {
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + "db6"),
                           new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         assertNotNull(info);
         assertEquals(BackupManager.FULL_AND_INCREMENTAL, info.getBackupType().intValue());
         assertNotNull(info.getStartedTime());
         assertNotNull(info.getFinishedTime());
         assertEquals(ShortInfo.RESTORE, info.getType().intValue());
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
         assertEquals("db6", info.getRepositoryName());
         assertNotNull(info.getBackupConfig());

         assertNotNull(repositoryService.getRepository("db6"));
         Session sessin_ws = repositoryService.getRepository("db6").login(credentials, "ws");
         assertNotNull(sessin_ws);
         assertNotNull(sessin_ws.getRootNode());
      }
   }

   public void testDropRepository() throws Exception
   {

      assertNotNull(repositoryService.getRepository("db5"));

      for (String workspaceName : repositoryService.getRepository("db5").getWorkspaceNames())
         forceCloseSession("db5", workspaceName);

      try
      {
         repositoryService.removeRepository("db5");
      }
      catch (Exception e)
      {
         fail();
      }
   }

   private int forceCloseSession(String repositoryName, String workspaceName) throws RepositoryException,
            RepositoryConfigurationException
   {
      ManageableRepository mr = repositoryService.getRepository(repositoryName);
      WorkspaceContainerFacade wc = mr.getWorkspaceContainer(workspaceName);

      SessionRegistry sessionRegistry = (SessionRegistry) wc.getComponent(SessionRegistry.class);

      return sessionRegistry.closeSessions(workspaceName);
   }

   @Override
   protected WorkspaceEntry makeWorkspaceEntry(WorkspaceEntry defWEntry, String repoNmae, String wsName,
            String sourceName)
   {
      WorkspaceEntry ws1back = new WorkspaceEntry();
      ws1back.setName(wsName);
      ws1back.setUniqueName(repoNmae + "_" + wsName);

      ws1back.setAccessManager(defWEntry.getAccessManager());
      ws1back.setAutoInitializedRootNt(defWEntry.getAutoInitializedRootNt());
      ws1back.setAutoInitPermissions(defWEntry.getAutoInitPermissions());
      ws1back.setCache(defWEntry.getCache());
      ws1back.setLockManager(defWEntry.getLockManager());

      // Indexer
      ArrayList qParams = new ArrayList();
      qParams.add(new SimpleParameterEntry("index-dir", "target" + File.separator + wsName));
      QueryHandlerEntry qEntry = new QueryHandlerEntry(defWEntry.getQueryHandler().getType(), qParams);

      ws1back.setQueryHandler(qEntry);

      ArrayList params = new ArrayList();
      for (Iterator i = defWEntry.getContainer().getParameters().iterator(); i.hasNext();)
      {
         SimpleParameterEntry p = (SimpleParameterEntry) i.next();
         SimpleParameterEntry newp = new SimpleParameterEntry(p.getName(), p.getValue());

         if (newp.getName().equals("source-name"))
            newp.setValue(sourceName);
         else if (newp.getName().equals("swap-directory"))
            newp.setValue("target/temp/swap/" + wsName);
         else if (newp.getName().equals("multi-db"))
            newp.setValue("false");

         params.add(newp);
      }

      ContainerEntry ce = new ContainerEntry(defWEntry.getContainer().getType(), params);
      ws1back.setContainer(ce);

      return ws1back;
   }

   protected RepositoryEntry makeRepositoryEntry(RepositoryEntry defREntry, String repoName,
            ArrayList<WorkspaceEntry> wsEntries)
   {
      RepositoryEntry rEntry = new RepositoryEntry();
      rEntry.setAccessControl(defREntry.getAccessControl());
      rEntry.setAuthenticationPolicy(defREntry.getAuthenticationPolicy());
      rEntry.setDefaultWorkspaceName(defREntry.getDefaultWorkspaceName());
      rEntry.setName(repoName);
      rEntry.setSecurityDomain(defREntry.getSecurityDomain());
      rEntry.setSessionTimeOut(defREntry.getSessionTimeOut());
      rEntry.setSystemWorkspaceName(defREntry.getSystemWorkspaceName());
      rEntry.setWorkspaceEntries(wsEntries);

      return rEntry;
   }

   /**
    * Will be created the Object from JSON binary data.
    * 
    * @param cl
    *          Class
    * @param data
    *          binary data (JSON)
    * @return Object
    * @throws Exception
    *           will be generated Exception
    */
   private Object getObject(Class cl, byte[] data) throws Exception
   {
      JsonHandler jsonHandler = new JsonDefaultHandler();
      JsonParser jsonParser = new JsonParserImpl();
      InputStream inputStream = new ByteArrayInputStream(data);
      jsonParser.parse(inputStream, jsonHandler);
      JsonValue jsonValue = jsonHandler.getJsonObject();

      return new BeanBuilder().createObject(cl, jsonValue);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ExtendedBackupManager getBackupManager()
   {
      return (ExtendedBackupManager) container.getComponentInstanceOfType(BackupManager.class);
   }

   protected void waitWorkspaceRestore(String repoName, String wsName) throws Exception
   {
      boolean wait = true;

      while (wait)
      {

         // Get restore info to workspace /<repoName>/<wsName>
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + repoName + "/"
                           + wsName), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         if (info.getState().intValue() == JobWorkspaceRestore.RESTORE_SUCCESSFUL
                  || info.getState().intValue() == JobWorkspaceRestore.RESTORE_FAIL)
         {
            wait = false;
         }
         else
         {
            Thread.sleep(500);
         }
      }
   }

   protected void waitRepositoryRestore(String repoName) throws Exception
   {
      boolean wait = true;

      while (wait)
      {
         // Get restore info
         MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
         ContainerRequestUserRole creq =
                  new ContainerRequestUserRole("GET", new URI(HTTP_BACKUP_AGENT_PATH
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/"
                           + repoName), new URI(""), null, new InputHeadersMap(headers));

         ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
         ContainerResponse cres = new ContainerResponse(responseWriter);
         handler.handleRequest(creq, cres);

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo) getObject(DetailedInfo.class, responseWriter.getBody());

         if (info.getState().intValue() == JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL
                  || info.getState().intValue() == JobRepositoryRestore.REPOSITORY_RESTORE_FAIL)
         {
            wait = false;
         }
         else
         {
            Thread.sleep(500);
         }
      }
   }
}
