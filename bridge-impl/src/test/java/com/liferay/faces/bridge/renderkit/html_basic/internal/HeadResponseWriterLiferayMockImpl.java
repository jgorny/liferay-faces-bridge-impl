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
package com.liferay.faces.bridge.renderkit.html_basic.internal;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.ResponseWriter;

import org.w3c.dom.Node;

import com.liferay.faces.bridge.util.internal.XMLUtil;


/**
 * @author  Kyle Stiemann
 */
public class HeadResponseWriterLiferayMockImpl extends HeadResponseWriterCompatImpl {

	// Private Data Members
	private String nodeAsString;

	public HeadResponseWriterLiferayMockImpl(ResponseWriter wrappedResponseWriter) {
		super(wrappedResponseWriter, null);
	}

	@Override
	protected void writeNodeToHeadSection(Node node, UIComponent componentResource) throws IOException {

		if (isElement(node)) {
			nodeAsString = XMLUtil.elementToString(node, false);
		}
		else {
			nodeAsString = XMLUtil.nodeToString(node);
		}
	}

	/* package-private */ String getLastNodeAsString() {
		return nodeAsString;
	}
}
