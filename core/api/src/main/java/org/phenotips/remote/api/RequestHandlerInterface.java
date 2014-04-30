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
package org.phenotips.remote.api;

import org.phenotips.data.PatientRepository;

import org.hibernate.Session;

import com.xpn.xwiki.XWikiContext;

import net.sf.json.JSONObject;

/**
 * Todo fixme.
 *
 * The object that is used by the core client (and server) to handle common operations upon a
 * {@link org.phenotips.remote.api.RequestInterface}.
 * Any implementation of this interface should get it's data through the constructor, and utilize the ability to have
 * several constructors to perform different types of operations.
 */
public interface RequestHandlerInterface<T extends RequestInterface>
{
    /**
     * @return either a new request, or the one that is already attached to this handler
     */
    T getRequest();

    Long saveRequest(Session session) throws Exception;

    T loadRequest(Long id, PatientRepository patientService);

    Boolean mail(XWikiContext context, MultiTaskWrapperInterface<IncomingSearchRequestInterface, JSONObject> wrapper);
}
