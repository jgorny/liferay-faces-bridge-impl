/**
 * Copyright (c) 2000-2021 Liferay, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.liferay.faces.bridge.application;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import javax.faces.application.Resource;
import javax.faces.context.FacesContext;


/**
 * @author  Kyle Stiemann
 */
public final class ResourceMockImpl extends Resource {

	// Private Final Data Members
	private final String requestPath;

	public ResourceMockImpl(String requestPath) {
		this.requestPath = requestPath;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		throw new UnsupportedOperationException("");
	}

	@Override
	public String getRequestPath() {
		return requestPath;
	}

	@Override
	public Map<String, String> getResponseHeaders() {
		throw new UnsupportedOperationException("");
	}

	@Override
	public URL getURL() {
		throw new UnsupportedOperationException("");
	}

	@Override
	public boolean userAgentNeedsUpdate(FacesContext context) {
		throw new UnsupportedOperationException("");
	}
}
