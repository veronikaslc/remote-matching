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
package org.phenotips.remote.common;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * This is where the constants and other configurations are stored for easy global access.
 */
public interface ApplicationConfiguration
{
    EntityReference XWIKI_SPACE = new EntityReference("XWiki", EntityType.SPACE);

    EntityReference PHENOMECENTRAL_SPACE = new EntityReference("PhenomeCentral", EntityType.SPACE);

    EntityReference USER_OBJECT_REFERENCE = new EntityReference("XWikiUsers", EntityType.DOCUMENT);

    EntityReference XWIKI_PREFERENCES_DOCUMENT_REFERENCE =
        new EntityReference("XWikiPreferences", EntityType.DOCUMENT, XWIKI_SPACE);

    EntityReference REMOTE_CONFIGURATION_OBJECT_REFERENCE =
        new EntityReference("RemoteMatchingServiceConfiguration", EntityType.DOCUMENT, PHENOMECENTRAL_SPACE);

    // XWiki remote request/config
    String CONFIGDOC_REMOTE_SERVER_NAME = "humanReadableName";

    String CONFIGDOC_REMOTE_KEY_FIELD = "remoteAuthToken";     // used for accessing remote server
    String CONFIGDOC_LOCAL_KEY_FIELD  = "localAuthToken";      // used for accessing this server

    String CONFIGDOC_REMOTE_BASE_URL_FIELD = "baseURL";

    EntityReference REMOTE_REQUEST_REFERENCE = new EntityReference("RemoteRequest", EntityType.DOCUMENT,
        new EntityReference("PhenomeCentral", EntityType.SPACE));

    String MAIL_SENDER = "mailsender";

    // TODO. This should be changed to the actual address.
    String EMAIL_FROM_ADDRESS = "antonkats@gmail.com";

    // TODO. Make the subject include the name of the current installation.
    String EMAIL_SUBJECT = "PhenomeCentral has found matches to your request";

    /** Document which can be relied upon to exist at all times. Needed for the REST server to work */
    EntityReference ABSOLUTE_DOCUMENT_REFERENCE =
        new EntityReference("XWikiPreferences", EntityType.DOCUMENT, XWIKI_SPACE);

    String REST_DEFAULT_USER_SPACE = "PhenomeCentral";

    String REST_DEFAULT_USER_NAME = "DefaultRemoteUser";
}