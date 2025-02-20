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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;
import javax.portlet.PortalContext;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import com.liferay.faces.bridge.component.internal.ComponentUtil;
import com.liferay.faces.bridge.context.BridgePortalContext;
import com.liferay.faces.bridge.context.HeadResponseWriterFactory;
import com.liferay.faces.util.application.ResourceUtil;
import com.liferay.faces.util.logging.Logger;
import com.liferay.faces.util.logging.LoggerFactory;
import com.liferay.faces.util.product.Product;
import com.liferay.faces.util.product.ProductFactory;


/**
 * This class is a JSF renderer that is designed for use with the h:head component tag. Portlets are forbidden from
 * rendering the <head>...</head> section, which is what is done by the JSF implementation's version of this renderer.
 * This renderer avoids rendering the <head>...</head> section and instead delegates that responsibility to the portal.
 *
 * @author  Neil Griffin
 */
public class HeadRendererBridgeImpl extends Renderer {

	// Private Constants
	private static final String FIRST_FACET = "first";
	private static final String MIDDLE_FACET = "middle";
	private static final String LAST_FACET = "last";

	// Logger
	private static final Logger logger = LoggerFactory.getLogger(HeadRendererBridgeImpl.class);

	@Override
	public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
		// no-op because Portlets are forbidden from rendering the <head>...</head> section.
	}

	@Override
	public void encodeChildren(FacesContext facesContext, UIComponent uiComponent) throws IOException {

		// Build up a list of components that are intended for the <head> section of the portal page.
		UIViewRoot uiViewRoot = facesContext.getViewRoot();
		List<UIComponent> headResources = new ArrayList<UIComponent>();

		// Add the list of components that are to appear first.
		List<UIComponent> firstResources = getFirstResources(facesContext, uiComponent);

		if (firstResources != null) {
			headResources.addAll(firstResources);
		}

		// Sort the components that are in the view root into stylesheets, scripts, and other.
		List<UIComponent> headComponentResources = uiViewRoot.getComponentResources(facesContext, "head");
		ExternalContext externalContext = facesContext.getExternalContext();
		final Product BOOTSFACES = ProductFactory.getProductInstance(externalContext, Product.Name.BOOTSFACES);
		final boolean BOOTSFACES_DETECTED = BOOTSFACES.isDetected();
		List<UIComponent> styleSheetResources = new ArrayList<UIComponent>();
		List<UIComponent> scriptResources = new ArrayList<UIComponent>();
		List<UIComponent> otherHeadResources = new ArrayList<UIComponent>();

		for (UIComponent headComponentResource : headComponentResources) {

			if (RenderKitUtil.isStyleSheetResource(headComponentResource, BOOTSFACES_DETECTED) ||
					isInlineStyleSheet(headComponentResource)) {
				styleSheetResources.add(headComponentResource);
			}
			else if (RenderKitUtil.isScriptResource(headComponentResource, BOOTSFACES_DETECTED) ||
					isInlineScript(headComponentResource)) {
				scriptResources.add(headComponentResource);
			}
			else {

				// Other head resources include <base>, <meta>, and <noscript> elments as well as passthrough <link>,
				// <style>, and <script> elements.
				otherHeadResources.add(headComponentResource);
			}
		}

		// Sort children into stylesheets, scripts, and other.
		List<UIComponent> children = uiComponent.getChildren();

		for (UIComponent child : children) {

			if (RenderKitUtil.isStyleSheetResource(child, BOOTSFACES_DETECTED) || isInlineStyleSheet(child)) {
				styleSheetResources.add(child);
			}
			else if (RenderKitUtil.isScriptResource(child, BOOTSFACES_DETECTED) || isInlineScript(child)) {
				scriptResources.add(child);
			}
			else {

				// Other head resources include <base>, <meta>, and <noscript> elments as well as passthrough <link>,
				// <style>, and <script> elements.
				otherHeadResources.add(child);
			}
		}

		if (!otherHeadResources.isEmpty()) {
			headResources.addAll(otherHeadResources);
		}

		// Add the list of stylesheet components that are in the view root.
		if (!styleSheetResources.isEmpty()) {
			headResources.addAll(styleSheetResources);
		}

		// Add the list of components that are to appear in the middle.
		List<UIComponent> middleResources = getMiddleResources(facesContext, uiComponent);

		if (middleResources != null) {
			headResources.addAll(middleResources);
		}

		// Add the list of script components that are in the view root.
		if (!scriptResources.isEmpty()) {
			headResources.addAll(scriptResources);
		}

		// Add the list of components that are to appear last.
		List<UIComponent> lastResources = getLastResources(facesContext, uiComponent);

		if (lastResources != null) {
			headResources.addAll(lastResources);
		}

		List<UIComponent> headResourcesToRenderInBody = new ArrayList<UIComponent>();
		PortletRequest portletRequest = (PortletRequest) externalContext.getRequest();
		PortalContext portalContext = portletRequest.getPortalContext();
		Iterator<UIComponent> iterator = headResources.iterator();

		while (iterator.hasNext()) {

			UIComponent headResource = iterator.next();

			// If the portlet container does not have the ability to add the resource to the <head> section of the
			// portal page, then
			if (!ableToAddResourceToHead(portalContext, headResource, BOOTSFACES_DETECTED)) {

				// Add it to the list of resources that are to be rendered in the body section by the body renderer.
				headResourcesToRenderInBody.add(headResource);

				// Remove it from the list of resources that are to be rendered in the head section by this renderer.
				iterator.remove();

				if (logger.isDebugEnabled()) {

					Map<String, Object> componentResourceAttributes = headResource.getAttributes();

					logger.debug(
						"Relocating resource to body: name=[{0}] library=[{1}] rendererType=[{2}] value=[{3}] className=[{4}]",
						componentResourceAttributes.get("name"), componentResourceAttributes.get("library"),
						headResource.getRendererType(), ComponentUtil.getComponentValue(headResource),
						headResource.getClass().getName());
				}
			}
		}

		// Save the list of resources that are to be rendered in the body section so that the body renderer can find it.
		Map<Object, Object> facesContextAttributes = facesContext.getAttributes();
		facesContextAttributes.put(RenderKitUtil.HEAD_RESOURCES_TO_RENDER_IN_BODY, headResourcesToRenderInBody);

		if (!headResources.isEmpty()) {

			// Save a temporary reference to the ResponseWriter provided by the FacesContext.
			ResponseWriter responseWriterBackup = facesContext.getResponseWriter();

			// Replace the ResponseWriter in the FacesContext with a HeadResponseWriter that knows how to write to
			// the <head>...</head> section of the rendered portal page.
			ResponseWriter headResponseWriter = (ResponseWriter) portletRequest.getAttribute(
					"com.liferay.faces.bridge.HeadResponseWriter");

			if (headResponseWriter == null) {

				PortletResponse portletResponse = (PortletResponse) externalContext.getResponse();
				PortletContext portletContext = (PortletContext) externalContext.getContext();
				headResponseWriter = HeadResponseWriterFactory.getHeadResponseWriterInstance(responseWriterBackup,
						portletContext, portletResponse);
			}

			portletRequest.setAttribute("com.liferay.faces.bridge.HeadResponseWriter", headResponseWriter);
			facesContext.setResponseWriter(headResponseWriter);

			Set<String> headResourceIds = RenderKitUtil.getHeadResourceIds(facesContext);

			for (UIComponent headResource : headResources) {

				headResource.encodeAll(facesContext);

				if (RenderKitUtil.isScriptResource(headResource, BOOTSFACES_DETECTED) ||
						RenderKitUtil.isStyleSheetResource(headResource, BOOTSFACES_DETECTED)) {
					headResourceIds.add(ResourceUtil.getResourceId(headResource));
				}
			}

			// Restore the temporary ResponseWriter reference.
			facesContext.setResponseWriter(responseWriterBackup);
		}
	}

	@Override
	public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {
		// no-op because Portlets are forbidden from rendering the <head>...</head> section.
	}

	@Override
	public boolean getRendersChildren() {
		return true;
	}

	protected List<UIComponent> getFirstResources(FacesContext facesContext, UIComponent uiComponent) {

		List<UIComponent> resources = null;

		UIComponent firstFacet = uiComponent.getFacet(FIRST_FACET);

		if (firstFacet != null) {
			resources = new ArrayList<UIComponent>();
			resources.add(firstFacet);
		}

		return resources;
	}

	protected List<UIComponent> getLastResources(FacesContext facesContext, UIComponent uiComponent) {

		List<UIComponent> resources = null;

		UIComponent firstFacet = uiComponent.getFacet(LAST_FACET);

		if (firstFacet != null) {
			resources = new ArrayList<UIComponent>();
			resources.add(firstFacet);
		}

		return resources;
	}

	protected List<UIComponent> getMiddleResources(FacesContext facesContext, UIComponent uiComponent) {

		List<UIComponent> resources = null;

		UIComponent firstFacet = uiComponent.getFacet(MIDDLE_FACET);

		if (firstFacet != null) {
			resources = new ArrayList<UIComponent>();
			resources.add(firstFacet);
		}

		return resources;
	}

	private boolean ableToAddResourceToHead(PortalContext portalContext, UIComponent componentResource,
		final boolean BOOTSFACES_DETECTED) {

		if (RenderKitUtil.isStyleSheetResource(componentResource, BOOTSFACES_DETECTED)) {
			return (portalContext.getProperty(BridgePortalContext.ADD_STYLE_SHEET_RESOURCE_TO_HEAD_SUPPORT) != null);
		}
		else if (RenderKitUtil.isScriptResource(componentResource, BOOTSFACES_DETECTED)) {
			return (portalContext.getProperty(BridgePortalContext.ADD_SCRIPT_RESOURCE_TO_HEAD_SUPPORT) != null);
		}
		else if (isInlineStyleSheet(componentResource)) {
			return (portalContext.getProperty(BridgePortalContext.ADD_STYLE_SHEET_TEXT_TO_HEAD_SUPPORT) != null);
		}
		else if (isInlineScript(componentResource)) {
			return (portalContext.getProperty(BridgePortalContext.ADD_SCRIPT_TEXT_TO_HEAD_SUPPORT) != null);
		}
		else {
			return (portalContext.getProperty(BridgePortalContext.ADD_ELEMENT_TO_HEAD_SUPPORT) != null);
		}
	}

	private boolean isInlineScript(UIComponent componentResource) {

		Map<String, Object> componentResourceAttributes = componentResource.getAttributes();
		String resourceName = (String) componentResourceAttributes.get("name");
		String rendererType = componentResource.getRendererType();

		return (componentResource instanceof InlineScript) ||
			((resourceName == null) &&
				(RenderKitUtil.SCRIPT_RENDERER_TYPE.equals(rendererType) ||

					// Avoid matching the exact renderer type to ensure that all Liferay Faces resources (including
					// potential future resource components) will correctly be detected.
					((rendererType != null) && rendererType.startsWith("com.liferay.faces.") &&
						rendererType.endsWith(".component.outputscript.OutputScriptRenderer"))));
	}

	private boolean isInlineStyleSheet(UIComponent componentResource) {

		Map<String, Object> componentResourceAttributes = componentResource.getAttributes();
		String resourceName = (String) componentResourceAttributes.get("name");
		String rendererType = componentResource.getRendererType();

		return (resourceName == null) &&
			(RenderKitUtil.STYLESHEET_RENDERER_TYPE.equals(rendererType) ||

				// Avoid matching the exact renderer type to ensure that all Liferay Faces resources (including
				// potential future resource components) will correctly be detected.
				((rendererType != null) && rendererType.startsWith("com.liferay.faces.") &&
					rendererType.endsWith(".component.outputstylesheet.OutputStylesheetRenderer")));
	}
}
