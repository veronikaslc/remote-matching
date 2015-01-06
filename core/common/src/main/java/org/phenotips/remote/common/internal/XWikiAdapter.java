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
package org.phenotips.remote.common.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.internal.PhenoTipsPatient;
import org.phenotips.remote.common.ApplicationConfiguration;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import java.net.InetAddress;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * TODO.
 */
public class XWikiAdapter
{
    private static final Logger logger = LoggerFactory.getLogger(XWikiAdapter.class);

    static public BaseObject getSubmitter(String userId, XWiki wiki, XWikiContext context,
        DocumentReferenceResolver<String> resolver) throws XWikiException
    {
        // Fixme. Does not check if the document is a user, but will return null if not.
        // Fixme. [possible] Does not check if the patient belongs to the user?

        EntityReference userReference = resolver.resolve(userId);
        return wiki.getDocument(userReference, context).getXObject(ApplicationConfiguration.USER_OBJECT_REFERENCE);
    }

    static public BaseObject getRemoteConfiguration(String baseURL, XWiki wiki, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument configurationsDoc =
            wiki.getDocument(ApplicationConfiguration.XWIKI_PREFERENCES_DOCUMENT_REFERENCE, context);
        if (configurationsDoc == null) {
            logger.error("Could not find configurations document");
        }
        List<BaseObject> configurations =
            configurationsDoc.getXObjects(ApplicationConfiguration.REMOTE_CONFIGURATION_OBJECT_REFERENCE);
        for (BaseObject remote : configurations) {
            if (remote == null) {
                continue;
            }
            String url = remote.getStringValue(ApplicationConfiguration.CONFIGDOC_REMOTE_BASE_URL_FIELD);
            if (StringUtils.equalsIgnoreCase(url, baseURL)) {
                logger.info("Matched configuration with URL: " + url);
                return remote;
            }
        }
        logger.error(
            "Could not find any remote configuration objects or no match was found. Configurations list size: " +
                configurations.size());
        // FIXME. Not exactly true.
        throw new XWikiException();
    }

    static public Patient getPatient(XWikiDocument doc)
    {
        return new PhenoTipsPatient(doc);
    }

    static public Patient getPatient(String patientId, XWiki wiki, XWikiContext context) throws XWikiException
    {
        return new PhenoTipsPatient(getPatientDoc(patientId, wiki, context));
    }

    static public XWikiDocument getPatientDoc(String patientId, XWiki wiki, XWikiContext context) throws XWikiException
    {
        EntityReference patientReference =
            new EntityReference(patientId, EntityType.DOCUMENT, Patient.DEFAULT_DATA_SPACE);

        return wiki.getDocument(patientReference, context);
    }

    static public BaseObject getRemoteConfigurationGivenRemoteIP(String remoteIP, XWikiContext context)
    {
        try {
            XWikiDocument prefsDoc =
                    context.getWiki().getDocument(ApplicationConfiguration.XWIKI_PREFERENCES_DOCUMENT_REFERENCE, context);

            List<BaseObject> remotes =
                    prefsDoc.getXObjects(ApplicationConfiguration.REMOTE_CONFIGURATION_OBJECT_REFERENCE);

            if (remotes == null) {
                logger.error("Remote matching admin section is absent or empty: can not process incoming request");
                return null;
            }

            logger.error("Request IP: {}", remoteIP);
            logger.debug("The number of remote configurations: {}", remotes.size());

            for (BaseObject remote : remotes) {
                if (remote == null) {
                    continue;
                }
                try {
                    String configuredURL = remote.getStringValue(ApplicationConfiguration.CONFIGDOC_REMOTE_BASE_URL_FIELD);
                    String configuredIP  = InetAddress.getByName(new URL(configuredURL).getHost()).getHostAddress();
                    logger.debug("Next server: {},  ip: {}", configuredURL, configuredIP);
                    if (StringUtils.equalsIgnoreCase(remoteIP, configuredIP)) {
                        return remote;
                    }
                } catch (MalformedURLException ex) {
                    logger.error("One of the configured remote matching servers has an incorrectly formatted URL=[{}]: {}",
                                 remote.getStringValue(ApplicationConfiguration.CONFIGDOC_REMOTE_BASE_URL_FIELD),
                                 ex.getMessage());
                }
            }
        } catch (Exception ex) {
            logger.warn("Error while getting server info for IP [{}]: [{}] {}", remoteIP, ex.getMessage(), ex);
        }
        return null;
    }
}