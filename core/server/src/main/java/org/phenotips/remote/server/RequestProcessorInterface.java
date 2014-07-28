/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
package org.phenotips.remote.server;

import org.xwiki.component.annotation.Role;
import org.xwiki.context.ExecutionContext;

import java.util.concurrent.ExecutorService;

import javax.servlet.http.HttpServletRequest;

import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

/**
 * TODO fix the doc
 */
@Role
public interface RequestProcessorInterface
{
    JSONObject processHTTPRequest(String json, ExecutorService queue, HttpServletRequest httpRequest) throws Exception;

    JSONObject internalProcessing(String json, ExecutorService queue, BaseObject configurationObject,
        ExecutionContext executionContext) throws Exception;
}
