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
package com.liferay.faces.bridge.context.internal;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;


/**
 * This class is an implementation of {@link FacesContext} that can be used during session expiration.
 *
 * @author  Neil Griffin
 */
public final class FacesContextExpirationImpl extends FacesContextUnsupportedImpl {

	// Private Data Members
	ExternalContext externalContext;

	public FacesContextExpirationImpl(ExternalContext externalContext) {
		this.externalContext = externalContext;

		setCurrentInstance(this);
	}

	@Override
	public ExternalContext getExternalContext() {
		return externalContext;
	}

	@Override
	public void release() {
		externalContext = null;
		setCurrentInstance(null);
	}
}
