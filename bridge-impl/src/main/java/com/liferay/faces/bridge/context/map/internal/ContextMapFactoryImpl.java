/**
 * Copyright (c) 2000-2016 Liferay, Inc. All rights reserved.
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
package com.liferay.faces.bridge.context.map.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.portlet.ClientDataRequest;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;

import com.liferay.faces.bridge.context.BridgeContext;
import com.liferay.faces.bridge.context.ContextMapFactory;
import com.liferay.faces.bridge.model.UploadedFile;
import com.liferay.faces.bridge.model.internal.UploadedFileBridgeImpl;
import com.liferay.faces.bridge.scope.BridgeRequestScope;
import com.liferay.faces.util.context.map.FacesRequestParameterMap;
import com.liferay.faces.util.context.map.MultiPartFormData;
import com.liferay.faces.util.product.ProductConstants;
import com.liferay.faces.util.product.ProductMap;


/**
 * @author  Neil Griffin
 */
public class ContextMapFactoryImpl extends ContextMapFactory {

	// Private Constants
	private static final boolean ICEFACES_DETECTED = ProductMap.getInstance().get(ProductConstants.ICEFACES)
		.isDetected();
	private static final String MULTIPART_FORM_DATA_FQCN = MultiPartFormData.class.getName();

	@Override
	public Map<String, Object> getApplicationScopeMap(BridgeContext bridgeContext) {
		return new ApplicationScopeMap(bridgeContext);
	}

	protected FacesRequestParameterMap getFacesRequestParameterMap(BridgeContext bridgeContext) {

		FacesRequestParameterMap facesRequestParameterMap = null;
		PortletRequest portletRequest = bridgeContext.getPortletRequest();
		PortletResponse portletResponse = bridgeContext.getPortletResponse();
		String namespace = portletResponse.getNamespace();
		BridgeRequestScope bridgeRequestScope = bridgeContext.getBridgeRequestScope();
		String defaultRenderKitId = bridgeContext.getDefaultRenderKitId();
		Map<String, String> facesViewParameterMap = getFacesViewParameterMap(bridgeContext);

		if (portletRequest instanceof ClientDataRequest) {

			ClientDataRequest clientDataRequest = (ClientDataRequest) portletRequest;
			String contentType = clientDataRequest.getContentType();

			// Note: ICEfaces ace:fileEntry relies on its own mechanism for handling file upload.
			if (!ICEFACES_DETECTED && (contentType != null) && contentType.toLowerCase().startsWith("multipart/")) {

				MultiPartFormData multiPartFormData = (MultiPartFormData) portletRequest.getAttribute(
						MULTIPART_FORM_DATA_FQCN);

				if (multiPartFormData == null) {
					facesRequestParameterMap = new FacesRequestParameterMapImpl(namespace, bridgeRequestScope,
							facesViewParameterMap, defaultRenderKitId);

					MultiPartFormDataProcessor multiPartFormDataProcessor = new MultiPartFormDataProcessorImpl();
					Map<String, List<com.liferay.faces.util.model.UploadedFile>> uploadedFileMap =
						multiPartFormDataProcessor.process(clientDataRequest, bridgeContext.getPortletConfig(),
							facesRequestParameterMap);

					multiPartFormData = new MultiPartFormDataImpl(facesRequestParameterMap, uploadedFileMap);

					// Save the multipart/form-data in a request attribute so that it can be referenced later-on in the
					// JSF lifecycle by file upload component renderers.
					portletRequest.setAttribute(MULTIPART_FORM_DATA_FQCN, multiPartFormData);
				}
				else {
					facesRequestParameterMap = multiPartFormData.getFacesRequestParameterMap();
				}
			}
		}

		if (facesRequestParameterMap == null) {
			Map<String, String[]> parameterMap = portletRequest.getParameterMap();
			facesRequestParameterMap = new FacesRequestParameterMapImpl(parameterMap, namespace, bridgeRequestScope,
					facesViewParameterMap, defaultRenderKitId);
		}

		return facesRequestParameterMap;
	}

	@Override
	public Map<String, String> getFacesViewParameterMap(BridgeContext bridgeContext) {

		String facesViewQueryString = bridgeContext.getFacesViewQueryString();

		return new FacesViewParameterMap(facesViewQueryString);
	}

	@Override
	public Map<String, String> getInitParameterMap(PortletContext portletContext) {
		return Collections.unmodifiableMap(new InitParameterMap(portletContext));
	}

	@Override
	public Map<String, Object> getRequestCookieMap(BridgeContext bridgeContext) {
		PortletRequest portletRequest = bridgeContext.getPortletRequest();
		Cookie[] cookies = portletRequest.getCookies();

		return new RequestCookieMap(cookies);
	}

	@Override
	public Map<String, String> getRequestHeaderMap(BridgeContext bridgeContext) {
		return new RequestHeaderMap(getRequestHeaderValuesMap(bridgeContext));
	}

	@Override
	public Map<String, String[]> getRequestHeaderValuesMap(BridgeContext bridgeContext) {
		return new RequestHeaderValuesMap(bridgeContext);
	}

	@Override
	public Map<String, String> getRequestParameterMap(BridgeContext bridgeContext) {

		FacesRequestParameterMap facesRequestParameterMap = getFacesRequestParameterMap(bridgeContext);

		return new RequestParameterMap(facesRequestParameterMap);
	}

	@Override
	public Map<String, String[]> getRequestParameterValuesMap(BridgeContext bridgeContext) {

		FacesRequestParameterMap facesRequestParameterMap = getFacesRequestParameterMap(bridgeContext);

		return new RequestParameterValuesMap(facesRequestParameterMap);
	}

	@Override
	public Map<String, Object> getRequestScopeMap(BridgeContext bridgeContext) {
		return new RequestScopeMap(bridgeContext);
	}

	@Override
	public Map<String, Object> getServletContextAttributeMap(ServletContext servletContext) {
		return new ServletContextAttributeMap(servletContext);
	}

	@Override
	public Map<String, Object> getSessionScopeMap(BridgeContext bridgeContext, int scope) {
		return new SessionScopeMap(bridgeContext, scope);
	}

	@Override
	public Map<String, List<UploadedFile>> getUploadedFileMap(BridgeContext bridgeContext) {

		Map<String, List<UploadedFile>> bridgeUploadedFileMap = null;
		PortletRequest portletRequest = bridgeContext.getPortletRequest();
		MultiPartFormData multiPartFormData = (MultiPartFormData) portletRequest.getAttribute(MULTIPART_FORM_DATA_FQCN);

		if (multiPartFormData != null) {
			Map<String, List<com.liferay.faces.util.model.UploadedFile>> uploadedFileMap =
				multiPartFormData.getUploadedFileMap();

			if (uploadedFileMap != null) {
				bridgeUploadedFileMap = new HashMap<String, List<UploadedFile>>(uploadedFileMap.size());

				Set<Map.Entry<String, List<com.liferay.faces.util.model.UploadedFile>>> entrySet =
					uploadedFileMap.entrySet();

				for (Map.Entry<String, List<com.liferay.faces.util.model.UploadedFile>> mapEntry : entrySet) {
					List<com.liferay.faces.util.model.UploadedFile> uploadedFileList = mapEntry.getValue();

					if (uploadedFileList != null) {
						List<UploadedFile> bridgeUploadedFileList = new ArrayList<UploadedFile>(
								uploadedFileList.size());

						for (com.liferay.faces.util.model.UploadedFile uploadedFile : uploadedFileList) {
							bridgeUploadedFileList.add(new UploadedFileBridgeImpl(uploadedFile));
						}

						bridgeUploadedFileMap.put(mapEntry.getKey(), bridgeUploadedFileList);
					}
				}

			}
		}

		return bridgeUploadedFileMap;
	}

	// Java 1.6+ @Override
	public ContextMapFactoryImpl getWrapped() {

		// Since this is the factory instance provided by the bridge, it will never wrap another factory.
		return null;
	}
}
