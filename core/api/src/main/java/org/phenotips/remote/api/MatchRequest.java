/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.remote.api;

import java.util.Date;

import net.sf.json.JSONObject;

/**
 *
 */
public interface MatchRequest
{
    /**
     * The id of the other server (for both incoming and outgoing requests)
     *
     * @return
     */
    String getRemoteServerId();

    /**
     *
     * @return
     */
    JSONObject getRequestJSON();

    /**
     *
     * @return
     */
    JSONObject getResponseJSON();

    /**
     *
     * @return
     */
    Date getRequestTime();

    /**
     *
     * @return
     */
    String getApiVersionUsed();
}
