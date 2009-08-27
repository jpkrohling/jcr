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
package org.exoplatform.services.ftp.command;

import org.exoplatform.services.ftp.FtpConst;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>
 * 
 * @version $Id: $
 */

public class CmdStat extends FtpCommandImpl
{

   public CmdStat()
   {
      commandName = FtpConst.Commands.CMD_STAT;
   }

   public static final String[] eXoStatInfo =
      {"211-", "", "     _/_/_/  _/_/_/  _/_/_/      _/_/_/  _/_/_/  _/_/   _/_/_/",
         "    _/        _/    _/   _/     _/        _/   _/   _/   _/   ",
         "   _/_/      _/    _/_/_/        _/      _/   _/ _/_/   _/    ",
         "  _/        _/    _/         _/_/_/     _/   _/   _/   _/     ",
         "  ____________________________________________________________", "  Connected from: [127.0.0.1]",
         "  Logged in as: [admin]", "  TYPE: ASCII", "  STRUcture: File", "  MODE: Stream", "  SYSTEM: Unix L8",
         "  CLIENT-SIDE-ENCODING: WINDOWS-1251", "  ____________________________________  http://eXoPlatform.org", "",
         "211 -"};

   public void run(String[] params) throws IOException
   {
      for (int i = 0; i < eXoStatInfo.length; i++)
      {
         reply(eXoStatInfo[i]);
      }
   }

}
