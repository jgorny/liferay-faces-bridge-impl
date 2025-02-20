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
package com.liferay.faces.bridge.scope.internal;

import javax.faces.FacesWrapper;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpSession;


/**
 * @author  Neil Griffin
 */
public abstract class BridgeRequestScopeManagerWrapper implements BridgeRequestScopeManager,
	FacesWrapper<BridgeRequestScopeManager> {

	public abstract BridgeRequestScopeManager getWrapped();

	public void removeBridgeRequestScopesByPortlet(PortletConfig portletConfig) {
		getWrapped().removeBridgeRequestScopesByPortlet(portletConfig);
	}

	public void removeBridgeRequestScopesBySession(HttpSession httpSession) {
		getWrapped().removeBridgeRequestScopesBySession(httpSession);
	}
}
